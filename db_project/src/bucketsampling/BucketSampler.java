package bucketsampling;

import common.WeightedSampler;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class BucketSampler implements WeightedSampler {

    private final Map<Integer, Bucket> bucketById = new HashMap<>();
    private final Map<String, Location> locationByItem = new HashMap<>();
    private long totalWeight;

    public BucketSampler(Map<String, Integer> weightByItem) {
        buildBuckets(weightByItem);
    }

    private void buildBuckets(Map<String, Integer> weightByItem) {
        for (Map.Entry<String, Integer> entry : weightByItem.entrySet()) {
            insert(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void insert(String item, int weight) {
        int bucketId = bucketIdFor(weight);
        Bucket bucket = bucketById.get(bucketId);
        if (bucket == null) {
            bucket = new Bucket();
            bucketById.put(bucketId, bucket);
        }
        locationByItem.put(item, new Location(bucketId, bucket.items.size()));
        bucket.items.add(item);
        bucket.weights.add(weight);
        bucket.totalWeight += weight;
        totalWeight += weight;
    }

    @Override
    public void delete(String item) {
        Location location = locationByItem.remove(item);
        if (location == null) {
            return;
        }
        Bucket bucket = bucketById.get(location.bucketId());
        int removedWeight = bucket.weights.get(location.position());
        fillGapWithLastItem(bucket, location);
        bucket.totalWeight -= removedWeight;
        totalWeight -= removedWeight;
        if (bucket.items.isEmpty()) {
            bucketById.remove(location.bucketId());
        }
    }

    private void fillGapWithLastItem(Bucket bucket, Location gap) {
        int lastPosition = bucket.items.size() - 1;
        String lastItem = bucket.items.remove(lastPosition);
        int lastWeight = bucket.weights.remove(lastPosition);
        if (gap.position() < lastPosition) {
            bucket.items.set(gap.position(), lastItem);
            bucket.weights.set(gap.position(), lastWeight);
            locationByItem.put(lastItem, gap);
        }
    }

    @Override
    public String sample(Random random) {
        int bucketId = pickBucketByWeight(random);
        return pickItemInsideBucket(bucketId, random);
    }

    private int pickBucketByWeight(Random random) {
        long target = random.nextLong(totalWeight);
        long weightOfLowerBuckets = totalWeight;
        for (int bucketId = bucketIdFor(totalWeight); bucketId >= 0; bucketId--) {
            Bucket bucket = bucketById.get(bucketId);
            if (bucket != null) {
                weightOfLowerBuckets -= bucket.totalWeight;
                if (target >= weightOfLowerBuckets) {
                    return bucketId;
                }
            }
        }
        throw new IllegalStateException("A vödrök összsúlya nem egyezik a teljes összsúllyal");
    }

    private String pickItemInsideBucket(int bucketId, Random random) {
        Bucket bucket = bucketById.get(bucketId);
        long bucketUpperLimit = 1L << (bucketId + 1);
        while (true) {
            int position = random.nextInt(bucket.items.size());
            long point = random.nextLong(bucketUpperLimit);
            if (point < bucket.weights.get(position)) {
                return bucket.items.get(position);
            }
        }
    }

    private int bucketIdFor(long weight) {
        int bucketId = 0;
        long halvedWeight = weight;
        while (halvedWeight >= 2) {
            halvedWeight /= 2;
            bucketId++;
        }
        return bucketId;
    }
}
