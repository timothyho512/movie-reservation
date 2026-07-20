package com.example.moviereservation.service;

import com.example.moviereservation.entity.Movie;
import com.example.moviereservation.entity.ProgrammeEntry;
import com.example.moviereservation.repository.MovieRepository;
import com.example.moviereservation.repository.ProgrammeEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class WeeklyProgrammeService {
    public static final int PROGRAMME_SIZE = 4;
    public static final int PROGRAMME_DAYS = 7;
    private static final int MINIMUM_RUN_WEEKS = 2;
    private static final int RECENT_RELEASE_DAYS = 35;
    private static final Logger logger = LoggerFactory.getLogger(WeeklyProgrammeService.class);

    private final MovieRepository movieRepository;
    private final ProgrammeEntryRepository programmeEntryRepository;

    public WeeklyProgrammeService(
            MovieRepository movieRepository,
            ProgrammeEntryRepository programmeEntryRepository
    ) {
        this.movieRepository = movieRepository;
        this.programmeEntryRepository = programmeEntryRepository;
    }

    @Transactional
    public List<ProgrammeEntry> ensureProgramme(LocalDate startsOn) {
        List<ProgrammeEntry> existing = programmeEntryRepository.findProgrammeStartingOn(startsOn);
        if (existing.size() >= PROGRAMME_SIZE) {
            return existing;
        }

        LocalDate endsOn = startsOn.plusDays(PROGRAMME_DAYS - 1L);
        List<Movie> selected = existing.stream()
                .map(ProgrammeEntry::getMovie)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        List<ProgrammeEntry> previous = previousProgramme(startsOn);
        Set<Long> alreadySelected = movieIds(selected);
        previous.stream()
                .map(ProgrammeEntry::getMovie)
                .filter(Movie::isActive)
                .filter(movie -> !alreadySelected.contains(movie.getId()))
                .limit(PROGRAMME_SIZE - selected.size())
                .forEach(movie -> {
                    selected.add(movie);
                    alreadySelected.add(movie.getId());
                });

        List<Movie> candidates = movieRepository.findAllByActiveTrueAndNowPlayingTrueOrderByTitleAsc();
        if (candidates.isEmpty()) {
            candidates = movieRepository.findAllByActiveTrueOrderByTitleAsc();
        }

        fillOpenPositions(selected, candidates, endsOn);
        if (selected.size() == PROGRAMME_SIZE && existing.isEmpty() && previous.size() == PROGRAMME_SIZE) {
            replaceOneEligibleMovie(selected, previous, candidates, startsOn, endsOn);
        }

        Set<Long> existingMovieIds = movieIds(existing.stream().map(ProgrammeEntry::getMovie).toList());
        List<ProgrammeEntry> newEntries = selected.stream()
                .filter(movie -> !existingMovieIds.contains(movie.getId()))
                .limit(PROGRAMME_SIZE - existing.size())
                .map(movie -> new ProgrammeEntry(movie, startsOn, endsOn))
                .toList();
        if (!newEntries.isEmpty()) {
            programmeEntryRepository.saveAll(newEntries);
        }

        List<ProgrammeEntry> result = programmeEntryRepository.findProgrammeStartingOn(startsOn);
        logger.info(
                "event=weekly_programme_ensured startsOn={} endsOn={} movieCount={} created={}",
                startsOn, endsOn, result.size(), newEntries.size()
        );
        return result;
    }

    private List<ProgrammeEntry> previousProgramme(LocalDate startsOn) {
        return programmeEntryRepository.findTopByStartsOnBeforeOrderByStartsOnDesc(startsOn)
                .map(entry -> programmeEntryRepository.findProgrammeStartingOn(entry.getStartsOn()))
                .orElseGet(List::of);
    }

    private void fillOpenPositions(List<Movie> selected, List<Movie> candidates, LocalDate programmeEndsOn) {
        Set<Long> selectedIds = movieIds(selected);
        candidates.stream()
                .filter(movie -> !selectedIds.contains(movie.getId()))
                .filter(movie -> movie.getReleaseDate() == null || !movie.getReleaseDate().isAfter(programmeEndsOn))
                .sorted(candidateComparator())
                .limit(PROGRAMME_SIZE - selected.size())
                .forEach(movie -> {
                    selected.add(movie);
                    selectedIds.add(movie.getId());
                });
    }

    private void replaceOneEligibleMovie(
            List<Movie> selected,
            List<ProgrammeEntry> previous,
            List<Movie> candidates,
            LocalDate startsOn,
            LocalDate endsOn
    ) {
        Set<Long> selectedIds = movieIds(selected);
        Set<Long> previouslyProgrammedIds = new HashSet<>(programmeEntryRepository.findAllProgrammedMovieIds());
        Movie replacement = candidates.stream()
                .filter(movie -> !selectedIds.contains(movie.getId()))
                .filter(movie -> isRecentReleaseFor(movie, startsOn, endsOn))
                .sorted(Comparator
                        .comparing((Movie movie) -> previouslyProgrammedIds.contains(movie.getId()))
                        .thenComparing(candidateComparator())
                )
                .findFirst()
                .orElse(null);
        if (replacement == null) {
            return;
        }

        List<ProgrammeEntry> twoWeeksAgo = programmeEntryRepository.findProgrammeStartingOn(startsOn.minusWeeks(2));
        Set<Long> twoWeeksAgoMovieIds = movieIds(twoWeeksAgo.stream().map(ProgrammeEntry::getMovie).toList());
        Movie retiring = previous.stream()
                .map(ProgrammeEntry::getMovie)
                .filter(movie -> twoWeeksAgoMovieIds.contains(movie.getId()))
                .sorted(Comparator
                        .comparing(Movie::isNowPlaying)
                        .thenComparing(Comparator.comparingInt(this::consecutiveRunWeeks).reversed())
                        .thenComparing(Movie::getTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .findFirst()
                .orElse(null);
        if (retiring == null || consecutiveRunWeeks(retiring) < MINIMUM_RUN_WEEKS) {
            return;
        }

        selected.removeIf(movie -> Objects.equals(movie.getId(), retiring.getId()));
        selected.add(replacement);
        logger.info(
                "event=weekly_programme_movie_replaced startsOn={} retiringMovieId={} replacementMovieId={}",
                startsOn, retiring.getId(), replacement.getId()
        );
    }

    private boolean isRecentReleaseFor(Movie movie, LocalDate startsOn, LocalDate endsOn) {
        LocalDate releaseDate = movie.getReleaseDate();
        return releaseDate != null
                && !releaseDate.isBefore(startsOn.minusDays(RECENT_RELEASE_DAYS))
                && !releaseDate.isAfter(endsOn);
    }

    private int consecutiveRunWeeks(Movie movie) {
        List<ProgrammeEntry> history = programmeEntryRepository.findAllByMovieIdOrderByStartsOnDesc(movie.getId());
        if (history.isEmpty()) {
            return 0;
        }
        int weeks = 1;
        for (int index = 1; index < history.size(); index++) {
            LocalDate expected = history.get(index - 1).getStartsOn().minusWeeks(1);
            if (!history.get(index).getStartsOn().equals(expected)) {
                break;
            }
            weeks++;
        }
        return weeks;
    }

    private Comparator<Movie> candidateComparator() {
        return Comparator
                .comparing(Movie::getReleaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Movie::getTitle, Comparator.nullsLast(String::compareToIgnoreCase));
    }

    private Set<Long> movieIds(List<Movie> movies) {
        Set<Long> ids = new HashSet<>();
        movies.stream().map(Movie::getId).filter(Objects::nonNull).forEach(ids::add);
        return ids;
    }
}
