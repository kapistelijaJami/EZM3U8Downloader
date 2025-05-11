package ezm3u8downloader;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import processes.Execute;
import processes.StreamGobbler;
import processes.StreamGobblerText;
import uilibrary.DragAndDrop;
import uilibrary.GameLoop;
import uilibrary.Window;

//TODO: maybe add yt-dlp for youtube videos specifically.
public class EZM3U8Downloader extends GameLoop {
	private final Window window;
	private final GUI gui;
	
	public boolean dropping = false;
	private int removeDropHereCounter = 10;
	public FFmpegProgress progress;
	public boolean downloadDone = false;
	
    public static void main(String[] args) {
		new EZM3U8Downloader(60).start();
    }
	
	public EZM3U8Downloader(int fps) {
		super(fps);
		
		this.window = new Window(1280, 720, "EZ M3U8 Downloader");
		gui = new GUI(this);
	}
	
	@Override
	protected void init() {
		window.setCanvasBackground(Color.DARK_GRAY);
		
		gui.init(window);
		
		window.setTransferHandler(new DragAndDrop(this::canImport, null, this::openLink));
		
		
		window.getCanvas().addKeyListener(new Input(this));
		
		window.setOnClosingFunction(this::shutdown);
		
		//Sets the look and feel for the save window to be from OS (like windows 10 etc).
		try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
	}
	
	@Override
	public void shutdown() {
		cancelDownload();
		super.shutdown();
	}
	
	public boolean canImport(TransferSupport support) {
		if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
			return false;
		}
		
		//You could deny dropping of wrong types of urls, but then you would have to figure out how to allow not only m3u8 files, but other video files as well.
		/*try {
			String text = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
			System.out.println(text);
		} catch (Exception e) {
			System.err.println(e);
		}*/
		
		dropping = true;
		removeDropHereCounter = 10;
		
		return true;
	}
	
	public boolean openLink(String text) {
		dropping = false;
		downloadDone = false;
		
		try {
			if (Execute.programExists("ffmpeg -version")) {
				String filePath = chooseSaveLocation(new File(getDownloadsFolder(), getDefaultFileName(text) + ".mp4"));
				if (filePath == null) {
					return false;
				}
				
				String command = "ffmpeg -protocol_whitelist file,http,https,tcp,tls -i " + addQuotes(text) + " -c copy " + addQuotes(filePath); //TODO: add optional start time and duration/end time etc. just -ss and -t for dur
				System.out.println("Command: " + command);
				Process process = Execute.executeCommandGetProcess(command);
				
				progress = new FFmpegProgress(process);
				
				new StreamGobblerText(process.getErrorStream(), StreamGobbler.Type.ERROR, false, progress::setProgress).start();
				
				process.waitFor();
				process.destroy();
				if (progress.wasCanceled()) {
					downloadDone = false;
				} else {
					downloadDone = true;
					openFileLocation(filePath);
				}
				progress = null;
			} else {
				JOptionPane.showMessageDialog(
						window,
						"FFmpeg is not installed!\nInstall it first before using this program.",
						"FFmpeg is not installed",
						JOptionPane.ERROR_MESSAGE
				);
				return false;
			}
		} catch (HeadlessException | InterruptedException e) {
			System.err.println("Couldn't download the file.");
			e.printStackTrace();
		}
		
		return true;
	}
	
	private String chooseSaveLocation(File defaultFile) {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		fileChooser.setSelectedFile(defaultFile);
		
		FileNameExtensionFilter filter = new FileNameExtensionFilter("MP4 Files (*.mp4)", "mp4");
		fileChooser.setFileFilter(filter);

		String filePath;
		int choice = fileChooser.showSaveDialog(window);
		if (choice == JFileChooser.APPROVE_OPTION) {
			File selectedFile = fileChooser.getSelectedFile();

			if (selectedFile.exists()) {
				JOptionPane.showMessageDialog(
						window,
						"File already exists. Please choose a different name.",
						"File Exists",
						JOptionPane.WARNING_MESSAGE
				);
				return chooseSaveLocation(selectedFile); //File exists, recursively ask for save location again.
			} else {
				filePath = selectedFile.getAbsolutePath();
				
				if (!selectedFile.getName().toLowerCase().endsWith(".mp4")) {
					filePath = filePath + ".mp4";
				}
				
				System.out.println("Selected save location: " + filePath);
				return filePath;
			}
		} else if (choice == JFileChooser.CANCEL_OPTION) {
			System.out.println("User canceled!");
			return null;
		}
		
		return null;
	}
	
	@Override
	protected void update() {
		if (dropping) {
			removeDropHereCounter--;
			if (removeDropHereCounter <= 0) {
				dropping = false;
			}
		}
	}
	
	@Override
	protected void render() {
		Graphics2D g = window.getGraphics2D();
		
		gui.render(g);
		
		window.display(g);
	}
	
	private static String addQuotes(String text) {
		return "\"" + text + "\"";
	}
	
	private static String getDefaultFileName(String url) {
		int lastSlashIndex = url.lastIndexOf('/');
        int lastDotIndex = url.lastIndexOf('.');
		
		if (lastSlashIndex >= 0 && lastDotIndex > lastSlashIndex) {
			return url.substring(lastSlashIndex + 1, lastDotIndex);
		} else if (lastSlashIndex >= 0) {
			return url.substring(lastSlashIndex + 1);
		}
		
		return "filename";
	}
	
	private static File getDefaultDirectory() {
        FileSystemView fileSystemView = FileSystemView.getFileSystemView();
        return fileSystemView.getDefaultDirectory();
    }
	
	private static File getDownloadsFolder() {
		String userHome = System.getProperty("user.home");
        File downloads = new File(userHome, "Downloads");
		if (downloads.exists() && downloads.isDirectory()) {
            return downloads;
        } else {
            return getDefaultDirectory();
        }
    }
	
	private static void openFileLocation(String filePath) {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			File file = new File(filePath);
			
			if (file.exists()) {
				try {
					desktop.open(file.getParentFile());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("File does not exist.");
			}
		} else {
			System.out.println("Desktop is not supported on this platform.");
		}
	}
	
	public void cancelDownload() {
		if (progress != null) {
			int option = JOptionPane.showConfirmDialog(null, "Are you sure you want to cancel the download?", "Confirmation", JOptionPane.OK_CANCEL_OPTION);
			if (option == JOptionPane.OK_OPTION) {
				progress.cancelProcess();
			}
		}
	}
}
