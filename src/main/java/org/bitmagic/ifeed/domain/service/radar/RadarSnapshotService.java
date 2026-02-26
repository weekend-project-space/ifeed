package org.bitmagic.ifeed.domain.service.radar;

import lombok.RequiredArgsConstructor;
import org.bitmagic.ifeed.domain.model.radar.RadarSnapshot;
import org.bitmagic.ifeed.domain.repository.radar.RadarSnapshotRepository;
import org.bitmagic.ifeed.domain.spec.RadarSnapshotSpecs;
import org.bitmagic.ifeed.exception.ApiException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RadarSnapshotService {

    private final RadarSnapshotRepository snapshotRepository;

    public RadarSnapshot resolveSnapshot(String snapshotId) {
        return resolveSnapshot(snapshotId, null);
    }

    public RadarSnapshot resolveSnapshot(String snapshotId, Integer windowHours) {
        Instant now = Instant.now();
        if (snapshotId == null || snapshotId.isBlank()) {
            if (windowHours != null) {
                return findLatestSnapshot(now, windowHours)
                        .or(() -> findLatestSnapshot(now, null))
                        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No radar snapshot available"));
            }

            return findLatestSnapshot(now, null)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No radar snapshot available"));
        }

        RadarSnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "SNAPSHOT_NOT_FOUND"));
        if (snapshot.getExpiresAt() != null && snapshot.getExpiresAt().isBefore(now)) {
            throw new ApiException(HttpStatus.GONE, "SNAPSHOT_EXPIRED");
        }
        return snapshot;
    }

    private Optional<RadarSnapshot> findLatestSnapshot(Instant now, Integer windowHours) {
        var page = snapshotRepository.findAll(
                RadarSnapshotSpecs.active(now, windowHours),
                PageRequest.of(0, 1, Sort.by("generatedAt").descending())
        );
        if (page.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(page.getContent().get(0));
    }
}
