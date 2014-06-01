package Tor61;

public class StreamTableKey {
	public String circuitID;
	public String streamID;
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((circuitID == null) ? 0 : circuitID.hashCode());
		result = prime * result
				+ ((streamID == null) ? 0 : streamID.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StreamTableKey other = (StreamTableKey) obj;
		if (circuitID == null) {
			if (other.circuitID != null)
				return false;
		} else if (!circuitID.equals(other.circuitID))
			return false;
		if (streamID == null) {
			if (other.streamID != null)
				return false;
		} else if (!streamID.equals(other.streamID))
			return false;
		return true;
	}

	public StreamTableKey(String circuitID, String streamID) {
		this.circuitID = circuitID;
		this.streamID = streamID;
	}
	
	public StreamTableKey(short circuitID, short streamID) {
		this.circuitID = "" + circuitID;
		this.streamID = "" + streamID;
	}
}
