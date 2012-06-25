package qat.components;
/**
 *
 * @author webhiker
 * @version 2.3, 17 June 1999
 */
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

import qat.common.*;
import qat.gui.*;

/**
 * This class implements a 3-part gauge displayed in red blue and green corersponding to tests
 * which have failed, unresolved or passed.
 * @author webhiker
 * @version %W %E
 */
public class Gauge extends JComponent {
    double min=0;
    double max=100;
    Color passedColor, failedColor, unresolvedColor, notrunColor, pendingColor; 
    // these variables are declared here instead of inside the paint loop, to save time
    int width=0, height=0, boxHeight=0;
    int boxCount=0;
    double percentPassed=0, percentFailed=0, percentUnresolved=0, percentNotRun = 0, percentPending = 0;

    static final int boxWidth = 6; // the width of each box
    static final int boxGap   = 2; // the gap between each box
    static final int inset    = 2; // gap between top of this component and top and bottom of the progress bar
	
    public Gauge() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		setDoubleBuffered(false);
    }
    
    public void setValue(double p, double f, double u, double pe, double n) {
		setToolTipText(Resources.getString("passed")+":"+(int)p+", "+
					   Resources.getString("failed")+":"+(int)f+", "+
					   Resources.getString("unresolved")+":"+(int)u+", "+
					   Resources.getString("notrun")+":"+(int)n+", "+
					   Resources.getString("pending")+":"+(int)pe);
		setPassed(p);
		setFailed(f);
		setUnresolved(u);
		setNotRun(n);
		setPending(pe);
		repaint();
    }

    public void setMax(double m) {
        max = m;
    }
	
    public void setMin(double m) {
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getPassed() {
        return (percentPassed * max) * 0.01;
    }
	
    public void setPassed(double p) {
		percentPassed     = ((p * 100.0) / max);	
    }
    
    public double getFailed() {
        return (percentFailed * max) * 0.01;
    }
    
    public void setFailed(double f) {
		percentFailed     = ((f * 100.0) / max);	
    }
    
    public double getUnresolved() {
        return (percentUnresolved * max) * 0.01;
    }
    
    public void setUnresolved(double u) {
		percentUnresolved = ((u * 100.0) / max);	
    }
	
	public double getNotRun() {
        return (percentNotRun * max) * 0.01;
	}
    
	public void setNotRun(double n) {
		percentNotRun = ((n * 100.0) / max);	
	}
	
	public double getPending() {
        return (percentPending * max) * 0.01;
	}
    
	public void setPending(double n) {
		percentPending = ((n * 100.0) / max);	
	}
	
    public void setForeground(Color p, Color f, Color u, Color n, Color pe) {
		super.setForeground(p);
		passedColor = p;
		failedColor = f;
		unresolvedColor = u;
		notrunColor = n;
		pendingColor = pe;
    }
    
    public boolean isOptimizedDrawingEnabled() {
		return true;
    }

    public void paint(Graphics g) {
		if (g==null)
			return;
		// only set these variables if the component was resized
		if (width != getSize().width) {
			width = getSize().width;
			height = getSize().height;
			boxHeight = height - (inset*2);
			boxCount = width / (boxWidth+boxGap);
		}
	
		int paintBoxes = (int)Math.round( (percentPassed * boxCount) * 0.01);	
	
		int xOffset = 1;		
		// paint the passed bar
		for (int i=0; i < paintBoxes;i++){
			g.setColor(passedColor);
			g.fill3DRect(xOffset,inset,boxWidth,boxHeight,true);    
			// step to next box co-ords
			xOffset = xOffset+boxWidth+boxGap;
		}			
		
		// paint the unresolved bar
		paintBoxes = (int)Math.round( (percentUnresolved * boxCount) * 0.01);	
		for (int i=0; i < paintBoxes;i++){
			g.setColor(unresolvedColor);
			g.fill3DRect(xOffset,inset,boxWidth,boxHeight,true);
			// step to next box co-ords
			xOffset = xOffset+boxWidth+boxGap;
		}
	
		// paint the failed bar
		paintBoxes = (int)Math.round( (percentFailed * boxCount) * 0.01);
		for (int i=0; i < paintBoxes;i++){
			g.setColor(failedColor);
			g.fill3DRect(xOffset,inset,boxWidth,boxHeight,true);
			// step to next box co-ords
			xOffset = xOffset+boxWidth+boxGap;
		}
		
		// paint the pending bar
		paintBoxes = (int)Math.round( (percentPending * boxCount) * 0.01);
		for (int i=0; i < paintBoxes;i++){
			g.setColor(pendingColor);
			g.fill3DRect(xOffset,inset,boxWidth,boxHeight,true);
			// step to next box co-ords
			xOffset = xOffset+boxWidth+boxGap;
		}
		
		// paint the notrun bar
		paintBoxes = (int)Math.round( (percentNotRun * boxCount) * 0.01);
		for (int i=0; i < paintBoxes;i++){
			g.setColor(notrunColor);
			g.fill3DRect(xOffset,inset,boxWidth,boxHeight,true);
			// step to next box co-ords
			xOffset = xOffset+boxWidth+boxGap;
		}		
	
		// now clear remainder of the boxes in case the value decreased
		g.setColor(getBackground());
		g.fillRect(xOffset,inset,(boxWidth+boxGap)*boxCount,boxHeight+1);
		// throw away this graphics resource now
		g.dispose();
    }
}
