import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class ClientPane extends GridPane{
	private Socket socket = null;
	//InputStreams
	InputStream is = null;
	BufferedReader br = null;
	//OutputStreams
	OutputStream os = null;
	BufferedOutputStream bos = null;
	DataOutputStream dos = null;
	private String grayURL = "/api/GrayScale";
	private boolean connected;
	String rawImageName, oFileName;
	File imageFile;
	BufferedImage img = null;
	private ImageView imgViewTop;
	private Button btnConnect;
	private Button btnGrayScale;
	private Button btnUpload;
	private TextArea txtArea;
	private ImageView imgView;
	public ClientPane(Stage stage) {
		setupGUI();
		btnConnect.setOnAction(e->{
			try
			{
				socket = new Socket("localhost", 5000);
				txtArea.appendText("Client connected to the server\r\n");
				//Bind streams
				connected = true;
				is = socket.getInputStream();
				br = new BufferedReader(new InputStreamReader(is));
				os = socket.getOutputStream();
				bos = new BufferedOutputStream(os);
				dos = new DataOutputStream(bos);
			}catch(IOException io)
			{
				io.printStackTrace();
			}
		});
		
		btnGrayScale.setOnAction(e->{
			imageProcessing();
			detectObj();
			/*Image proImage = new Image("data/processedImg.jpg", 600,600,true,true);
			imgView.setImage(proImage);*/
		});
		btnUpload.setOnAction(e->{
			if(connected) {
				FileChooser fc = new FileChooser();
				fc.setTitle("Open Resource File");
				fc.setInitialDirectory(new File("data"));
				fc.getExtensionFilters().add(new ExtensionFilter("Image Files", "*.png","*.JPEG*", "*.jpg","*.gif"));
				File tmp =fc.showOpenDialog(stage);
				imageFile = tmp;
				String pImageFilename="file:";
				try {
					rawImageName = imageFile.getCanonicalPath();								
					pImageFilename+=rawImageName.substring(imageFile.getParent().lastIndexOf("\\")+1, rawImageName.length());
					oFileName=pImageFilename;
					System.out.println(pImageFilename);
					//imageURL.setText(rawImageName);
					//System.out.println(oFileName.substring(0, oFileName.lastIndexOf("\\")+1)+"temp"+oFileName.substring(oFileName.lastIndexOf("\\"),oFileName.lastIndexOf("."))+"type"+oFileName.substring(oFileName.lastIndexOf("."), oFileName.length())oFileName.substring(0, oFileName.lastIndexOf("\\")+1)+"temp"+oFileName.substring(oFileName.lastIndexOf("\\"),oFileName.lastIndexOf("."))+"type"+oFileName.substring(oFileName.lastIndexOf("."), oFileName.length()));
				} catch (IOException e1) {

					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				if (imageFile != null) {
					Image unProceesedImage= new Image(pImageFilename,600,600,true,true);
					imgViewTop.setImage(unProceesedImage);

				}
			}else {
				System.err.println("Connect To The Server");
			}
		});
	}
	
	private void imageProcessing()
	{
		String encodedFile = null;
		try {
			//read the file in FIS
			FileInputStream fisReader = new FileInputStream(imageFile);
			//saving the pixels colours
			/*img = ImageIO.read(imageFile);
			int width = img.getWidth();
			int height = img.getHeight();
			int p = img.getRGB(width-1, height-1);*/
			byte[] bytes = new byte[(int)imageFile.length()];
			fisReader.read(bytes);
			encodedFile = new String(Base64.getEncoder().encodeToString(bytes));
			byte[] bytesToSend = encodedFile.getBytes();
			//Send file to server
			//POST HTTP REQUEST
			dos.write(("POST " + grayURL +" HTTP/1.1\r\n").getBytes());
			dos.write(("Content-Type: applicaation/text\r\n").getBytes());
			dos.write(("Content-Length: "+ encodedFile.length() + "\r\n").getBytes());
			dos.write(("\r\n").getBytes());
			dos.write(bytesToSend);
			dos.write(("\r\n").getBytes());
			dos.flush();
			txtArea.appendText("Request send\r\n");
			//Read response
			String response = "";
			String line = "";
			while(!(line = br.readLine()).equals(""));
			{
				response += line + "\n";
			}
			System.out.println(response);
			//Receiving the response
			String imgData = "";
			while((line = br.readLine()) != null)
			{
				imgData += line;
			}
			//Extract the base64 string
			//
			//Decode the base64 string received
			String strBase64 = imgData.substring(imgData.indexOf('\'')+1, imgData.lastIndexOf('}')-1);
			//preProcessingString = strBase64;
			byte[] decodedString = Base64.getDecoder().decode(strBase64);
			//Display the image
			Image grayImg = new Image(new ByteArrayInputStream(decodedString), 600, 600, true, true);
			imgView.setImage(grayImg);
			socket.close();
		}catch(IOException io)
		{
			
		}
	}
	private void detectObj()
	{
		String filename = String.valueOf(imageFile);
		CascadeClassifier classifier = new CascadeClassifier("docs/haarcascade_frontalcatface.xml");
		Mat matrix = Imgcodecs.imread(filename);
		
		MatOfRect faceDetections = new MatOfRect();
		classifier.detectMultiScale(matrix, faceDetections);
		System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));
		txtArea.appendText("Detected "+faceDetections.toArray().length+" faces");
		//Drawing boxes
		for(Rect rect : faceDetections.toArray())
		{
			Imgproc.rectangle(matrix, new Point(rect.x, rect.y), 
					new Point(rect.x + rect.width, rect.y + rect.height), 
					new Scalar(0,0,255), 3);
		}
		
		Imgcodecs.imwrite("data/processedImg.jpg", matrix);
		/*Image proImage = new Image("data/processedImg.jpg");
		imgView.setImage(proImage);*/
	}
	private void setupGUI() {
		setHgap(10);
		setVgap(10);
		setAlignment(Pos.CENTER);
		
		imgViewTop = new ImageView();
		btnConnect = new Button("Connect");
		btnUpload = new Button("Upload Image");
		btnGrayScale = new Button("Process");
		txtArea = new TextArea();
		txtArea.setPrefHeight(50);
		imgView = new ImageView();
		Image readImg = new Image("file:/data/download.jpg");
		imgViewTop.setImage(readImg);
		add(imgViewTop, 0, 0);
		add(btnConnect, 0, 1);
		add(btnUpload, 2, 1);
		add(btnGrayScale, 1, 1);
		add(txtArea, 0, 2, 2, 1);
		add(imgView, 0, 3, 2, 1);
	}
}
