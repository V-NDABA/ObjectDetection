import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.*;
public class ClientMain extends Application{

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO Auto-generated method stub
		ClientPane root = new ClientPane(primaryStage);
		Scene scene = new Scene(root, 800, 700);
		primaryStage.setTitle("Project1.0");
		primaryStage.setScene(scene);
		primaryStage.show();
	}

}
