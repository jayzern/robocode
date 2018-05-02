package u1500212.additional;

import java.util.*;
import java.awt.geom.*;

public class EnemyInfo extends Point2D.Double
{
	// Used to fix compiler warning? Not very familiar with this, but doesnt affect code.
	private static final long serialVersionUID = 1L;

    long lastHit;
    double energy;
    double velocity;
    Vector<MeleeBullet> waves = new Vector<MeleeBullet>();
}   