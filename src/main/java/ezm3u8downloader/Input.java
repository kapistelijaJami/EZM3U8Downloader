package ezm3u8downloader;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;

public class Input implements KeyListener {
	private EZM3U8Downloader downloader;
	
	public Input(EZM3U8Downloader downloader) {
		this.downloader = downloader;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		if (e.isControlDown() && !e.isAltDown() && !e.isShiftDown() && e.getKeyCode() == 86) { //PASTE
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = clipboard.getContents(null);
			
			if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				try {
					String link = (String) transferable.getTransferData(DataFlavor.stringFlavor);
					new Thread(() -> downloader.openLink(link)).start(); //new Thread so the program still responds to cancel keypress
				} catch (UnsupportedFlavorException | IOException ex) {
					ex.printStackTrace();
				}
			}
		} else if (e.getKeyCode() == 127) { //Delete, cancel download
			downloader.cancelDownload();
		}
	}
	
	@Override
	public void keyReleased(KeyEvent e) {
		
	}
}
