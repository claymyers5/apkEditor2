import java.io.*;
import java.util.Enumeration;
import java.util.Stack;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.io.IOUtils;

import javafx.application.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
public class Main extends Application{

	public static void main(String[] args){
		launch();
	}
	
	ZipFile apk;
	InputStream editorInputStream;
	FileChooser imageChooser, apkChooser, keystoreChooser;
	Stage mainStage;
	Label apkName;
	TreeView<String> apkTreeView;
	BorderPane root;
	SplitPane mainPane, imagesPane;
	VBox apkBrowser, fileEditor;
	TextArea textEditor;
	ImageView topImageView, bottomImageView;
	Enumeration<? extends ZipEntry> entries;
	Image originalImage, userInputImage, defaultReplacementImage;
	BorderPane fileViewer;
	
	@Override
	public void start(Stage mainStage) throws Exception {
		//create window and set title
		this.mainStage = mainStage;
		mainStage.setTitle("APK Editor 0.01a");
		//init menubar
		MenuBar menuBar = new MenuBar();
		Menu fileMenu = new Menu("File");
		MenuItem open = new MenuItem("Open apk");
		open.setOnAction(new EventHandler<ActionEvent>(){
			@Override
			public void handle(ActionEvent arg0) {
				System.out.println("open clicked!");
					openApk();
			}
		});
		fileMenu.getItems().addAll(
				open,
				new MenuItem("Export"));
		menuBar.getMenus().addAll(fileMenu);
		//init filechoosers
		apkChooser = new FileChooser();
		apkChooser.setTitle("Select apk file");
		apkChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Android app package", "*.apk"), 
				new ExtensionFilter("Zip file", "*.zip"), 
				new ExtensionFilter("All files", "*.*"));
		keystoreChooser = new FileChooser();
		keystoreChooser.setTitle("Select keystore file");
		keystoreChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Jarsigner keystore", ".keystore"),
				new ExtensionFilter("All files", "*.*"));
		imageChooser = new FileChooser();
		imageChooser.setTitle("Select image file");
		imageChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Current image extension", "*.png"),
				new ExtensionFilter("PNG files", "*.png", "*.PNG"),
				new ExtensionFilter("JPG files", "*.jpg", "*.JPG"),
				new ExtensionFilter("GIF files", "*.gif", "*.GIF"),
				new ExtensionFilter("All files", "*.*")
				);		
		//create main splitpane
		mainPane = new SplitPane();
		mainPane.setOrientation(Orientation.HORIZONTAL);
		mainPane.setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
		//create apk browser
		apkBrowser = new VBox();
		apkName = new Label("No file open");
		apkTreeView = new TreeView<String>();
		VBox.setVgrow(apkTreeView, Priority.ALWAYS);
		apkBrowser.getChildren().addAll(apkName, apkTreeView);
		//create file editor
		textEditor = new TextArea("Text file will be loaded here");
		fileViewer = new BorderPane();
		fileViewer.setCenter(textEditor);
		defaultReplacementImage = new Image(new File("assets/replacement image.png").toURI().toString());
		topImageView = new ImageView(defaultReplacementImage);
		bottomImageView = new ImageView(defaultReplacementImage);
		imagesPane = new SplitPane();
		imagesPane.getItems().addAll(null, null);
		imagesPane.setOrientation(Orientation.VERTICAL);
		imagesPane.setStyle("-fx-border-color:black; -fx-background-color: gray;");
		//fileEditor.getChildren().addAll(textEditor);
		//add nodes to mainPane
		mainPane.getItems().addAll(apkBrowser, fileViewer);
		mainPane.setDividerPositions(0.3, 0.7);
		//create borderpane as root so children fill window
		root = new BorderPane();
		root.setCenter(mainPane);
		root.setTop(menuBar);
		mainStage.setScene(new Scene(root, 1280, 720));
		mainStage.show();
	}	
	
	/**
	 * This method changes the default extension in the file chooser window
	 * to encourage the user to choose a file of the same type.
	 * @param extension The extension to set, in the format *.*
	 */
	private void changeCurrentImageExtension(String extension){
		imageChooser.getExtensionFilters().set(0, new ExtensionFilter("Current image extension", extension));
	}
	
	private void openApk(){
		//open file chooser
		File input = apkChooser.showOpenDialog(mainStage);
		//init ZipFile object
		apk = null;
		try {
			//open apk
			apk = new ZipFile(input);
		} catch (IOException e) {
			e.printStackTrace();
		}
		//set the text on the file browser label
		apkName.setText(input.getAbsolutePath());
		//initialize root element of tree
		TreeItem<String> rootItem = new TreeItem<String>(input.getName());
		//get list of entries in the zip file
		entries = apk.entries();
		//set tree from zip
		ZipEntry nextEntry;
		//create TreeView from root element
		TreeView<String> fileTree = new TreeView<String>(rootItem);
		while(entries.hasMoreElements()){
			nextEntry = entries.nextElement();
			String name = nextEntry.getName();
			System.out.println(name);
			String element = name;
			//set current node to root (this has to be done for every single file since the 
				//entries are full paths)
			TreeItem<String> currentNode = rootItem;
			//if file inside directory,
			while(name.contains("/")){
				//get name of directory or file
				element = name.substring(0, name.indexOf("/"));
				//remove element name from current path
				name = name.substring(name.indexOf("/")+1, name.length());
				boolean exists = false;
				for (TreeItem<String> child : currentNode.getChildren()){
					//check the currentNode's children to see if the path exists
					if (!exists && child.getValue().equals(element)){
						//if it exists, set currentNode to the existing path
						exists = true;
						currentNode = child;
					}
				}
				if (!exists){
					//if if doesnt exist, create the path, and then set current node to the new path
					TreeItem<String> tempNode = new TreeItem<String>(element);
					currentNode.getChildren().add(tempNode);
					currentNode = tempNode;
				}
			}
			//add file to appropriate directory
			currentNode.getChildren().add(new TreeItem<String>(name));
		}
		
		//set up the created treeview in the user interface
		apkTreeView = new TreeView<String>(rootItem);
		//add the appropriate listener for the treeview 
		apkTreeView.getSelectionModel().selectedItemProperty().addListener( new ChangeListener<TreeItem<String>>() {
			@Override
			public void changed(ObservableValue<? extends TreeItem<String>> observable, TreeItem<String> oldValue,
					TreeItem<String> newValue) {
						//get the name of the clicked file and pass to fileOpener method
						if (newValue.getValue().contains(".")){
							System.out.println(newValue.getValue() + " is a file! Constructing full path...");
							String path = "";
							TreeItem<String> currentNode = newValue;
							while(currentNode.getParent() instanceof TreeItem<?>){
								//if current node is parent of file, add / because it's a directory
								if (!currentNode.equals(newValue)) path = "/"+path;
								//add currentNode name to path
								path = currentNode.getValue() + path;
								//set currentNode to parent
								currentNode = currentNode.getParent();
							}
							openFileInEditor(path);
						}
						
			}

	      });
		apkBrowser.getChildren().set(1, apkTreeView);
		VBox.setVgrow(apkTreeView, Priority.ALWAYS);
	}
	
	private void openImage(){
		imageChooser.showOpenDialog(mainStage);
	}
	
	private void openKeystore(){
		keystoreChooser.showOpenDialog(mainStage);
	}
	
	private void openFileInEditor(String entryPath){
		try {
			System.out.println("Attempting to open file named " + entryPath);
			editorInputStream = apk.getInputStream(apk.getEntry(entryPath));
			//determine fileType
			//if image
			if (entryPath.contains(".png") || 
					entryPath.contains(".PNG") ||
					entryPath.contains(".jpg") ||
					entryPath.contains(".JPG") ||
					entryPath.contains(".gif") ||
					entryPath.contains(".GIF")){
				System.out.println("File at " + entryPath + " is an image!");
				//open image file
				originalImage = new Image(editorInputStream);
				//put into top imageview
				topImageView.setImage(originalImage);
				//set bottom imageview to replacement image
				bottomImageView.setImage(defaultReplacementImage);
				//put into pane
				imagesPane.getItems().set(0, topImageView);
				imagesPane.getItems().set(1, bottomImageView);
				imagesPane.setDividerPosition(0, 0.5);
				fileViewer.setCenter(imagesPane);
			}
			else{
				StringWriter writer = new StringWriter();
				IOUtils.copy(editorInputStream, writer, "UTF-8");
				String output = writer.toString();
				textEditor.setText(output);
				fileViewer.setCenter(textEditor);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
