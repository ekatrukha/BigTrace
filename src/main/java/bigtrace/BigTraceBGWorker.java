package bigtrace;

public interface BigTraceBGWorker {
	
	/** get current description of BG working task **/
	public String getProgressState();
	/** set current description of BG working task **/
	public void setProgressState(String state_);
	
}
