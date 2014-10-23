package org.codeisland.aggregato.service.storage;

/**
 * Objects implementing this interface provide a typed {@link #merge(Object)}-method,
 *  to merge Data from another Object into this one.
 * @author Lukas Knuth
 * @version 1.0
 */
public interface Mergeable<T> {

    /**
     * Merges the data from {@code other} with the data in this Object.
     * @param other the object to merge data from.
     */
    public void merge(T other);
}
