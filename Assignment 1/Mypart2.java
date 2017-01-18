import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class Mypart2 {
	static final int width=512,height=512;
	static int n=16;
	static double s=4;
	static double fps=10;
	//static BufferedImage imgs[]=new BufferedImage[360];
	public static void main(String[] args) {
		if(args.length==3){
			n=Integer.parseInt(args[0]);
			s=Double.parseDouble(args[1]);
			fps=Double.parseDouble(args[2]);
		}
		/*for(int i=0;i<360;i++){
			imgs[i]=BaseImage.getImage(width, height, n,(double)i);
		}*/
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
	        JFrame frame = new JFrame();
	        frame.setLayout(new GridLayout(1,2,1,1));        
	        OutputComp output=new OutputComp(fps);
	        InputPanel input=new InputPanel(n,s,output);
	        frame.add(input);
	        frame.add(output);      
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        frame.pack();
	        frame.setVisible(true);
	        new Thread(output).start();
            }
        });
        
	}

}

@SuppressWarnings("serial")
class InputPanel extends JPanel implements ActionListener{
	private int width=512,height=512;
	private int n=16;
	private double s=4;
	private Timer timer=new Timer(1000/60,this);
	private double theta=0;
	private OutputComp output;
	public InputPanel(int _n,double _s,OutputComp _output){
		n=_n;
		s=_s;
		output=_output;
		timer.start();
	}
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        BaseImage.paintImage(g2d, width, height, n, theta);
    }
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
	@Override
	public void actionPerformed(ActionEvent arg0) {
		theta+=360*s/60;
		this.repaint();
		this.paint(output.getImage().createGraphics());
	}
}

@SuppressWarnings("serial")
class OutputComp extends JComponent implements Runnable{
	int width=512,height=512;
	double fps=10;
	private BufferedImage image;
	public OutputComp(double _fps){
		fps=_fps;
		image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}
	public BufferedImage getImage(){
		return image;
	}
	@Override 
	public void paint(Graphics g){
		super.paint(g);
		Graphics2D g2d=(Graphics2D) g;
		g2d.drawImage(image,0,0,null);
	}
	@Override
	public void run() {
		while(true){
			this.repaint();
			try {
				Thread.sleep(Math.round(1000/fps));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
}

class BaseImage {
	static public void paintImage(Graphics2D g2d,int width,int height,int n,double dt){
		int x0=width/2,y0=height/2;
		g2d.setColor(new Color(255,255,255));
		g2d.fillRect(0, 0, width, height);		
		g2d.setColor(new Color(0,0,0,255));
		for(double i=0;i<360;i+=360/(double)n){
			int x1=(int)(x0+width/2*Math.cos(Math.toRadians(i+dt)));
			int y1=(int)(y0+height/2*Math.sin(Math.toRadians(i+dt)));
			g2d.drawLine(x0, y0, x1, y1);
		}
	}
   /* static public BufferedImage getImage(int width,int height,int n,double dt){
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		int x0=width/2,y0=height/2;
		g2d.setColor(new Color(255,255,255));
		g2d.fillRect(0, 0, width, height);		
		g2d.setColor(new Color(0,0,0,255));
		for(double i=0;i<360;i+=360/(double)n){
			int x1=(int)(x0+width/2*Math.cos(Math.toRadians(i+dt)));
			int y1=(int)(y0+height/2*Math.sin(Math.toRadians(i+dt)));
			g2d.drawLine(x0, y0, x1, y1);
		}
		return bi;
    }*/    	
}