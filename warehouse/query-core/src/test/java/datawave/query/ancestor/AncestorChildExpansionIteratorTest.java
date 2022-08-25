package datawave.query.ancestor;

import datawave.query.Constants;
import datawave.query.function.AncestorEquality;
import datawave.query.function.Equality;
import datawave.query.util.IteratorToSortedKeyValueIterator;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Range;
import org.apache.accumulo.core.data.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class AncestorChildExpansionIteratorTest {
    private static final List<String> children = Arrays.asList("a", "a.1", "a.1.1", "a.1.2", "a.1.2.1", "a.10", "a.2", "a.3", "a.4", "a.4.1", "a.4.1.1",
                    "a.4.1.2", "a.4.2", "a.5", "a.6", "a.7", "a.8", "a.9");
    
    private List<Map.Entry<Key,Value>> baseValues;
    private AncestorChildExpansionIterator iterator;
    private IteratorToSortedKeyValueIterator baseIterator;
    private Equality equality;
    
    @BeforeEach
    public void setup() {
        baseValues = new ArrayList<>();
        equality = new AncestorEquality();
        baseIterator = new IteratorToSortedKeyValueIterator(baseValues.iterator());
        iterator = new AncestorChildExpansionIterator(baseIterator, children, equality);
    }
    
    // basic iterator contract verification
    
    @Test
    public void testUninitializedHasTop() {
        Assertions.assertThrows(IllegalStateException.class, () -> iterator.hasTop());
    }
    
    @Test
    public void testUninitializedgetTopKey() {
        Assertions.assertThrows(IllegalStateException.class, () -> iterator.getTopKey());
    }
    
    @Test
    public void testUninitializedgetTopValue() {
        Assertions.assertThrows(IllegalStateException.class, () -> iterator.getTopValue());
    }
    
    @Test
    public void testUninitializedNext() {
        Assertions.assertThrows(IllegalStateException.class, () -> iterator.next());
    }
    
    @Test
    public void testSeekEnablesHasTop() throws IOException {
        iterator.seek(new Range(), Collections.emptyList(), false);
        Assertions.assertFalse(iterator.hasTop());
    }
    
    @Test
    public void testNoTopGetTopKeyError() throws IOException {
        iterator.seek(new Range(), Collections.emptyList(), false);
        Assertions.assertFalse(iterator.hasTop());
        Assertions.assertThrows(NoSuchElementException.class, () -> iterator.getTopKey());
    }
    
    @Test
    public void testNoTopGetTopValueError() throws IOException {
        iterator.seek(new Range(), Collections.emptyList(), false);
        Assertions.assertFalse(iterator.hasTop());
        Assertions.assertThrows(NoSuchElementException.class, () -> iterator.getTopValue());
    }
    
    @Test
    public void testNoTopNextError() throws IOException {
        iterator.seek(new Range(), Collections.emptyList(), false);
        Assertions.assertFalse(iterator.hasTop());
        Assertions.assertThrows(NoSuchElementException.class, () -> iterator.next());
    }
    
    // end basic iterator contract verification
    
    private static final String FI_ROW = "shard";
    private static final String FI_COLUMN_FAMILY = "fi" + Constants.NULL_BYTE_STRING + "field";
    private static final String FI_COLUMN_QUALIFIER_PREFIX = "value" + Constants.NULL_BYTE_STRING + "dataType" + Constants.NULL_BYTE_STRING;
    private static final String FI_VIS = "ABC";
    private static final long FI_TIMESTAMP = 1234567890;
    
    private Key generateFiKey(String uid) {
        return new Key(FI_ROW, FI_COLUMN_FAMILY, FI_COLUMN_QUALIFIER_PREFIX + uid, FI_VIS, FI_TIMESTAMP);
    }
    
    private void assertKey(Key key, String uid) {
        Assertions.assertNotNull(key);
        Assertions.assertEquals(key.getRow().toString(), FI_ROW);
        Assertions.assertEquals(key.getColumnFamily().toString(), FI_COLUMN_FAMILY);
        Assertions.assertEquals(key.getColumnQualifier().toString(), FI_COLUMN_QUALIFIER_PREFIX + uid);
        Assertions.assertEquals(new String(key.getColumnVisibilityParsed().getExpression()), FI_VIS);
        Assertions.assertEquals(key.getTimestamp(), FI_TIMESTAMP);
    }
    
    @Test
    public void testSingleChildMatch() throws IOException {
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.3"), new Value()));
        baseIterator = new IteratorToSortedKeyValueIterator(baseValues.iterator());
        iterator = new AncestorChildExpansionIterator(baseIterator, children, equality);
        
        iterator.seek(new Range(), Collections.emptyList(), false);
        
        Assertions.assertTrue(iterator.hasTop());
        Key topKey = iterator.getTopKey();
        assertKey(topKey, "a.3");
        Value topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertFalse(iterator.hasTop());
    }
    
    @Test
    public void testFamilyMatch() throws IOException {
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.1"), new Value()));
        baseIterator = new IteratorToSortedKeyValueIterator(baseValues.iterator());
        iterator = new AncestorChildExpansionIterator(baseIterator, children, equality);
        
        iterator.seek(new Range(), Collections.emptyList(), false);
        
        Assertions.assertTrue(iterator.hasTop());
        Key topKey = iterator.getTopKey();
        assertKey(topKey, "a.1");
        Value topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.2");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.2.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertFalse(iterator.hasTop());
    }
    
    @Test
    public void testFamilyMatchWithOverlaps() throws IOException {
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.1"), new Value()));
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.1.1"), new Value()));
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.1.2.1"), new Value()));
        baseIterator = new IteratorToSortedKeyValueIterator(baseValues.iterator());
        iterator = new AncestorChildExpansionIterator(baseIterator, children, equality);
        
        iterator.seek(new Range(), Collections.emptyList(), false);
        
        Assertions.assertTrue(iterator.hasTop());
        Key topKey = iterator.getTopKey();
        assertKey(topKey, "a.1");
        Value topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.2");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.2.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertFalse(iterator.hasTop());
    }
    
    @Test
    public void testChildIndexAdvanceOnIteratorNext() throws IOException {
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.3"), new Value()));
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.4.1"), new Value()));
        baseIterator = new IteratorToSortedKeyValueIterator(baseValues.iterator());
        iterator = new AncestorChildExpansionIterator(baseIterator, children, equality);
        
        iterator.seek(new Range(), Collections.emptyList(), false);
        
        Assertions.assertTrue(iterator.hasTop());
        Key topKey = iterator.getTopKey();
        assertKey(topKey, "a.3");
        Value topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.4.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.4.1.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.4.1.2");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertFalse(iterator.hasTop());
    }
    
    @Test
    public void testMultipleGappedRanges() throws IOException {
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.1"), new Value()));
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.3"), new Value()));
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.4.1.1"), new Value()));
        baseValues.add(new AbstractMap.SimpleImmutableEntry<>(generateFiKey("a.9"), new Value()));
        baseIterator = new IteratorToSortedKeyValueIterator(baseValues.iterator());
        iterator = new AncestorChildExpansionIterator(baseIterator, children, equality);
        
        iterator.seek(new Range(), Collections.emptyList(), false);
        
        Assertions.assertTrue(iterator.hasTop());
        Key topKey = iterator.getTopKey();
        assertKey(topKey, "a.1");
        Value topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.2");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.1.2.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.3");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.4.1.1");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertTrue(iterator.hasTop());
        topKey = iterator.getTopKey();
        assertKey(topKey, "a.9");
        topValue = iterator.getTopValue();
        Assertions.assertNotNull(topValue);
        
        iterator.next();
        Assertions.assertFalse(iterator.hasTop());
    }
}
