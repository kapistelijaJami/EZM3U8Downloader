package ezm3u8downloader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import uilibrary.Window;
import uilibrary.elements.Element;
import uilibrary.elements.TextElement;
import uilibrary.enums.Alignment;

public class GUI {
	private final EZM3U8Downloader downloader;
	private final List<Element> elements = new ArrayList<>();
	
	public Element dropHereText;
	public TextElement progressText;
	public TextElement estimatedTimeLeft;
	public TextElement cancelWithDeleteText;
	
	public GUI(EZM3U8Downloader downloader) {
		this.downloader = downloader;
	}
	
	public void init(Window window) {
		TextElement text = new TextElement("Drag and Drop or copy and paste M3U8 url.", new Color(245, 222, 179)).setFontSize(50);
		text.arrange(window.getCanvas()).align(Alignment.TOP).setMargin(0, "0.1h2");
		elements.add(text);
		
		TextElement text2 = new TextElement("(Also any type of video url works)", new Color(245, 222, 179)).setFontSize(30);
		text2.arrange(window.getCanvas()).align(Alignment.BOTTOM).setMargin(0, "0.1h2");
		elements.add(text2);
		
		dropHereText = new TextElement("Drop here!", Color.RED).setFontSize(50);
		dropHereText.arrange(window.getCanvas());
		
		progressText = new TextElement("", new Color(245, 222, 179)).setFontSize(50);
		progressText.arrange(window.getCanvas());
		
		estimatedTimeLeft = new TextElement("", new Color(245, 222, 179)).setFontSize(30);
		estimatedTimeLeft.arrange(window.getCanvas()).align(Alignment.TOP).setMargin(0, "0.5h2 + 5h");
		
		cancelWithDeleteText = new TextElement("(You can cancel the download with delete)", new Color(245, 222, 179)).setFontSize(25);
		cancelWithDeleteText.arrange(window.getCanvas()).align(Alignment.TOP, Alignment.RIGHT).setMargin(10);
	}
	
	public void render(Graphics2D g) {
		for (Element element : elements) {
			element.render(g);
		}
		
		if (downloader.dropping) {
			dropHereText.render(g);
		} else if (downloader.progress != null) {
			progressText.setText(downloader.progress.getProgress());
			progressText.render(g);
			
			String estimated = downloader.progress.getEstimatedTimeLeft();
			if (!estimated.isBlank()) {
				estimatedTimeLeft.setText("Estimated time left: " + estimated);
				estimatedTimeLeft.render(g);
			}
			
			cancelWithDeleteText.render(g);
		} else if (downloader.downloadDone) {
			progressText.setText("Download done.");
			progressText.render(g);
		}
	}
}
