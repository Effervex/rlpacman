package jCloisterZoneAlt;

public class CarcassonneEnvironment extends
		jCloisterZone.CarcassonneEnvironment {
	public CarcassonneEnvironment() {
		relationalWrapper_ = new CarcassonneRelationalWrapper();
	}
}
