/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package orekit.coverage.access;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import orekit.object.CoveragePoint;

/**
 *
 * @author nozomihitomi
 */
public class CoverageAccessMerger implements Serializable{
    private static final long serialVersionUID = 9046345352941432741L;

    /**
     * Merges the accesses in two sets of accesses. The inputs are two sets of
     * access computed at each point in the same coverage grid definition.
     *
     * @param accesses1 a set of accesses that is to be merged. Must have the
     * same coverage points as accesses2
     * @param accesses2 a set of accesses that is to be merged. Must have the
     * same coverage points as accesses1
     * @param andCombine true if accesses should be combined with logical AND
     * (i.e. intersection). False if accesses should be combined with a logical
     * OR (i.e. union)
     * @return the merged coverage access
     */
    public HashMap<CoveragePoint, TimeIntervalArray> mergeCoverageDefinitionAccesses(
            HashMap<CoveragePoint, TimeIntervalArray> accesses1,
            HashMap<CoveragePoint, TimeIntervalArray> accesses2,
            boolean andCombine) throws IllegalArgumentException {

        ArrayList<HashMap<CoveragePoint, TimeIntervalArray>> accessesCollection = new ArrayList<>(2);
        accessesCollection.add(accesses1);
        accessesCollection.add(accesses2);
        return this.mergeCoverageDefinitionAccesses(accessesCollection, andCombine);
    }

    /**
     * Merges multiple accesses from different simulations. The inputs are two
     * sets of access computed at each point in the same coverage grid
     * definition.
     *
     * @param accessesCollection a collection of accesses from multiple scenario
     * that is to be merged. Each item in collection must have the same coverage
     * points
     * @param andCombine true if accesses should be combined with logical AND
     * (i.e. intersection). False if accesses should be combined with a logical
     * OR (i.e. union)
     * @return the merged coverage access
     */
    public HashMap<CoveragePoint, TimeIntervalArray> mergeCoverageDefinitionAccesses(
            Collection<HashMap<CoveragePoint, TimeIntervalArray>> accessesCollection,
            boolean andCombine) throws IllegalArgumentException {

        Set<CoveragePoint> ptKeys = accessesCollection.iterator().next().keySet();
        HashMap<CoveragePoint, TimeIntervalArray> out = new HashMap<>(ptKeys.size());

        //Check that all accesses sets have the same coverage grid definition
        for (HashMap<CoveragePoint, TimeIntervalArray> accesses : accessesCollection) {
            if (!accesses.keySet().equals(ptKeys)) {
                throw new IllegalArgumentException("Failed to merge access for two sets of grid points. Expected grid points between sets to be equal. Found sets containing different points.");
            }
        }

        for (CoveragePoint pt : ptKeys) {
            ArrayList<TimeIntervalArray> accessArrays = new ArrayList<>();
            for (HashMap<CoveragePoint, TimeIntervalArray> accesses : accessesCollection) {
                accessArrays.add(accesses.get(pt));
            }
            TimeIntervalMerger merger = new TimeIntervalMerger(accessArrays);
            TimeIntervalArray mergedArray;
            if (andCombine) {
                mergedArray = merger.andCombine();
            } else {
                mergedArray = merger.orCombine();
            }
            out.put(pt, mergedArray);
        }
        return out;
    }
}
