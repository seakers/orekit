package seakers.orekit.event.detector;

import org.orekit.geometry.fov.FieldOfView;
import org.orekit.propagation.events.EventEnablingPredicateFilter;
import org.orekit.propagation.events.FieldOfViewDetector;
import seakers.orekit.object.CoveragePoint;

public class FilteredFOVBuilder {
    public static EventEnablingPredicateFilter<FieldOfViewDetector> createFilteredFOV(CoveragePoint pt, FieldOfView fov) {
        return new EventEnablingPredicateFilter<>(
                new FieldOfViewDetector(pt, fov),
                (state, eventDetector, g) -> {
                    CoveragePoint localCp = (CoveragePoint)eventDetector.getPVTarget();
                    final double trueElevation = localCp.getElevation(state.getPVCoordinates().getPosition(),
                            state.getFrame(), state.getDate());
                    return trueElevation > 0;
                });
    }
}
