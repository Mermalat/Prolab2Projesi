package classifier;

import model.UserRecord;
import java.util.List;

public interface IClassifier {
    void train(List<UserRecord> trainingData);
    String predict(UserRecord user);
    String getName();
}
