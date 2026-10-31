package bucketsampling;

import java.util.ArrayList;
import java.util.List;

class Bucket {
    final List<String> items = new ArrayList<>();
    final List<Integer> weights = new ArrayList<>();
    long totalWeight;
}
