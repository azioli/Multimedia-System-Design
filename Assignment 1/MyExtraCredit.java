import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;

public class MyExtraCredit {
	static final int width=512,height=512;
	static int n=64;
	static double s=4;
	static double fps=7;
	static double scale=2;
	static boolean aa=false;
	//static BufferedImage imgs[]=new BufferedImage[360];
	public static void main(String[] args) {
		if(args.length==5){
			n=Integer.parseInt(args[0]);
			s=Double.parseDouble(args[1]);
			fps=Double.parseDouble(args[2]);
			scale=Double.parseDouble(args[3]);
			if(1==Integer.parseInt(args[4]))
				aa=true;
		}
		/*for(int i=0;i<360;i++){
			imgs[i]=BaseImage.getImage(width, height, n,(double)i);
		}*/
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
	        JFrame frame = new JFrame();
	        frame.setLayout(new GridLayout(1,2,1,1)); 
	        //boolean ta=aa && fps<s*2;
	        boolean ta=aa;
	        OutputComp output=new OutputComp(fps,scale);
	        InputPanel input=new InputPanel(n,s,scale,aa,ta,output);
	        input.setOpaque(true);
	        output.setOpaque(true);
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
	private double scale=1.0;
	private boolean aa=false,ta=false;
	private BufferedImage blur=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
	private OutputComp output;
	public InputPanel(int _n,double _s,double _scale,boolean _aa,boolean _ta,OutputComp _output){
		n=_n;
		s=_s;
		scale=_scale;
		output=_output;
		aa=_aa;
		ta=_ta;
		timer.start();
	}
	@Override
	public void paintComponent(Graphics g){
		super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        BaseImage.paintImage(g2d, width, height, n, theta);
        if(ta){
        	blur=BaseImage.blurImage(width, height, n, theta);
        }
    }
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }
	@Override
	public void actionPerformed(ActionEvent arg0) {
		theta+=360*s/60;
		this.repaint();
		BufferedImage oimg=output.getImage();
		if(!aa){
		    BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(),oimg.getType());
			Graphics2D g2d=bi.createGraphics();
			this.paint(g2d);
			g2d.dispose();			
			
			for(int i=0;i<oimg.getWidth();i++)
				for(int j=0;j<oimg.getHeight();j++){
					oimg.setRGB(i, j, bi.getRGB((int)Math.round(i*scale), (int)Math.round(j*scale)));
			}
		}
		else{
		    BufferedImage bi = new BufferedImage(this.getWidth(), this.getHeight(),oimg.getType());
			if(!ta){
				Graphics2D g2d=bi.createGraphics();
				this.paint(g2d);
				g2d.dispose();
			}
			else{
				bi=blur;
			}
			if(scale==1.0 && !ta){
				oimg.getGraphics().drawImage(bi, 0, 0, null);
				return;
			}
			for(int i=0;i<oimg.getWidth();i++)
				for(int j=0;j<oimg.getHeight();j++){
					long sumR=0,sumG=0,sumB=0;
					int si=(int)(i*scale),sj=(int)(j*scale);
					int count=0;
					for(int m=si-1;m<=si+1;m++)
						for(int n=sj-1;n<=sj+1;n++){
							if(m>=0 && m<width && n>=0 && n<height){
								Color color=new Color(bi.getRGB(m, n));
								sumR+=color.getRed();
								sumG+=color.getGreen();
								sumB+=color.getBlue();
								count++;
							}
					}
					int pix = 0xff000000 | (((byte)(sumR/count) & 0xff) << 16) | (((byte)(sumG/count) & 0xff) << 8) | ((byte)(sumB/count) & 0xff);
					oimg.setRGB(i, j, pix);
				}
		}
	}
}

@SuppressWarnings("serial")
class OutputComp extends JPanel implements Runnable{
	int width=512,height=512;
	double fps=10;
	private BufferedImage image;
	private double scale=1.0;
	public OutputComp(double _fps,double _scale){
		fps=_fps;
		scale=_scale;
		width=(int)Math.round(width/scale);
		height=(int)Math.round(height/scale);
		image=new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	}
	public BufferedImage getImage(){
		return image;
	}
	@Override 
	public void paintComponent(Graphics g){
		super.paintComponent(g);
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
   static public BufferedImage blurImage(int width,int height,int n,double dt){
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = bi.createGraphics();
		int x0=width/2,y0=height/2;
		g2d.setColor(new Color(255,255,255));
		g2d.fillRect(0, 0, width, height);		
		for(double i=0;i<360;i+=360/(double)n){
			g2d.setColor(new Color(0,0,0,255*(int)i/360));
			int x1=(int)(x0+width/2*Math.cos(Math.toRadians(i+dt)));
			int y1=(int)(y0+height/2*Math.sin(Math.toRadians(i+dt)));
			g2d.drawLine(x0, y0, x1, y1);
		}
		return bi;
    }  	
}