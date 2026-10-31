package common;

import java.util.Random;

public interface WeightedSampler {

    void insert(String item, int weight);

    void delete(String item);

    String sample(Random random);
}
