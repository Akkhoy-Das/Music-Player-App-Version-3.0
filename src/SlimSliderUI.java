import javax.swing.*;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.*;

public class SlimSliderUI extends BasicSliderUI {
    private Color trackFill;
    private Color trackBg;

    public SlimSliderUI(JSlider slider, Color fill, Color bg) {
        super(slider);
        this.trackFill = fill;
        this.trackBg   = bg;
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle trackRect = this.trackRect;
        int cy = trackRect.y + trackRect.height / 2 - 2;
        // Background track
        g2.setColor(trackBg);
        g2.fillRoundRect(trackRect.x, cy, trackRect.width, 4, 4, 4);
        // Fill portion
        int fillW = (int)((slider.getValue() / (double)slider.getMaximum()) * trackRect.width);
        g2.setColor(trackFill);
        g2.fillRoundRect(trackRect.x, cy, fillW, 4, 4, 4);
        g2.dispose();
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Rectangle thumbRect = this.thumbRect;
        g2.setColor(trackFill);
        g2.fillOval(thumbRect.x + 2, thumbRect.y + 2, thumbRect.width - 4, thumbRect.height - 4);
        g2.setColor(Color.WHITE);
        int cx = thumbRect.x + thumbRect.width / 2;
        int cy = thumbRect.y + thumbRect.height / 2;
        g2.fillOval(cx - 4, cy - 4, 8, 8);
        g2.dispose();
    }
}
