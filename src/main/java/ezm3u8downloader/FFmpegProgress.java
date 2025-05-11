package ezm3u8downloader;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import timer.DurationFormat;

public class FFmpegProgress {
	private Duration totalDur;
	private Duration currentDur;
	private double progress;
	private final Process process;
	private boolean canceled = false;
	private final long startTime;
	
	private String estimatedTimeLeft = "";
	private long lastUpdate = -1;
	
	public FFmpegProgress(Process process) {
		this.process = process;
		this.startTime = System.currentTimeMillis();
	}
	
	public void setProgress(String text) {
		if (totalDur == null) {
			int idx = text.indexOf("Duration: ");
			if (idx == -1) {
				return;
			}
			
			text = text.substring(idx + 10);
			text = text.substring(0, text.indexOf(" "));
			totalDur = DurationFormat.parseSimple(text);
			return;
		}
		
		int idx = text.indexOf("time=");
		if (idx == -1) {
			return;
		}
		text = text.substring(idx + 5);
		text = text.substring(0, text.indexOf(" "));
		
		currentDur = DurationFormat.parseSimple(text);
		
		progress = currentDur.toMillis() * 100.0 / totalDur.toMillis(); //percentage
	}
	
	public String getProgress() {
		DurationFormat format = new DurationFormat("hh:mm:ss");
		String outputCurrent = format.format(Duration.ZERO);
		if (currentDur != null) {
			outputCurrent = format.format(currentDur);
		}
		
		String outputTotal = format.format(Duration.ZERO);
		if (totalDur != null) {
			outputTotal = format.format(totalDur);
		}
		return String.format(Locale.US, "Progress:  %.1f%% \t  " + outputCurrent + " / " + outputTotal, progress);
	}
	
	public void setDuration(Duration dur) {
		this.totalDur = dur;
	}
	
	public void cancelProcess() {
		canceled = true;
		List<ProcessHandle> list = process.descendants().collect(Collectors.toList());
		for (ProcessHandle p : list) {
			p.destroy();
		}
		process.destroy();
	}
	
	public boolean wasCanceled() {
		return canceled;
	}
	
	public String getEstimatedTimeLeft() {
		if (progress == 0) {
			return "";
		}
		
		if (lastUpdate != -1 && System.currentTimeMillis() - lastUpdate < 5000) {
			return estimatedTimeLeft;
		}
		
		lastUpdate = System.currentTimeMillis();
		long timePassed = (lastUpdate - startTime) / 1000;
		long secondsLeft = (long) (100 / progress * timePassed) - timePassed;
		
		if (secondsLeft == 0) {
			lastUpdate = -1;
			return "";
		}
		
		estimatedTimeLeft = DurationFormat.formatWithUnits(Duration.ofSeconds(secondsLeft));
		return estimatedTimeLeft;
	}
}
