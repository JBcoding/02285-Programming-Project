package karlMarx;

import java.util.Arrays;

public class FastSet<E> {
    private static final int INITIAL_SIZE = 0x10;
    private static final float DEFAULT_LOAD_FACTOR = 0.5f;

    private Object[] table; // length of this must always be a power of 2
    private int size;

    private float loadFactor;

    public FastSet() {
        this(INITIAL_SIZE, DEFAULT_LOAD_FACTOR);
    }

    public FastSet(int initialSize) {
        this(initialSize, DEFAULT_LOAD_FACTOR);
    }

    public FastSet(int initialSize, float loadFactor) {
        int initialSizeReal = INITIAL_SIZE;
        while (initialSizeReal < initialSize) {
            initialSizeReal *= 2;
        }
        this.loadFactor = loadFactor;
        this.size = 0;
        this.table = new Object[initialSizeReal];
    }

    private static int hash(int h) {
        h ^= (h >>> 20) ^ (h >>> 12);
        return h ^ (h >>> 7) ^ (h >>> 4);
    }

    private int getDefaultIndex(E o) {
        return getDefaultIndex(o, table);
    }

    private int getDefaultIndex(E o, Object[] table) {
        return hash(o.hashCode()) & (table.length - 1);
    }

    public boolean add(E o) {
        if (!contains(o)) {
            size ++;
            if (size > table.length * loadFactor) {
                resize(2 * table.length);
            }
            insert(o);
            return true;
        }
        return false;
    }

    private void resize(int size) {
        if (size < 16) {
            return;
        }
        Object[] newTable = new Object[size];
        for (Object o : table) {
            if (o != null) {
                insert(newTable, (E) o);
            }
        }
        table = newTable;
    }

    private void insert(Object[] table, E o) {
        int index = getDefaultIndex(o, table);
        while (table[index] != null) {
            index = (index + 1) % table.length;
        }
        table[index] = o;
    }

    private void insert(E o) {
        insert(table, o);
    }

    public boolean contains(E o) {
        return getRealIndex(o) != -1;
    }

    public int getRealIndex(E o) {
        int index = getDefaultIndex(o);
        int hashCode = o.hashCode();
        while (table[index] != null) {
            if (table[index].hashCode() == hashCode && o.equals(table[index])) {
                return index;
            }
            index = (index + 1) % table.length;
            System.err.println(index);
        }
        return -1;
    }

    public boolean remove(E o) {
        int realIndex = getRealIndex(o);
        if (realIndex != -1) {
            size --;
            table[realIndex] = null;
            System.err.println(realIndex);
            realIndex += (realIndex + 1) % table.length;
            while (table[realIndex] != null) {
                Object e = table[realIndex];
                table[realIndex] = null;
                insert((E) e);
                realIndex += (realIndex + 1) % table.length;
            }
            if (size < table.length * loadFactor / 2) {
                resize(table.length / 2);
            }
            return true;
        }
        return false;
    }

    public int size() {
        return size;
    }
}
