import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class Mypart1 {
	static final int width=512,height=512;
	static int n=16;
	static double scale=2.0;
	static boolean aa=false;
	public static void main(String[] args){
		if(args.length==3){
			n=Integer.parseInt(args[0]);
			scale=Double.parseDouble(args[1]);
			if(1==Integer.parseInt(args[2]))
				aa=true;
		}
		// TODO Auto-generated method stub
		BufferedImage input = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d=input.createGraphics();
		g2d.drawRect(0, 0, width, height);
		g2d.fillRect(0, 0, width, height);
		int x0=width/2,y0=height/2;
		g2d.setColor(Color.BLACK);
		for(double i=0;i<2*Math.PI;i+=2*Math.PI/n){
			int x1=(int)(x0+width*Math.cos(i));
			int y1=(int)(y0+height*Math.sin(i));
			g2d.drawLine(x0, y0, x1, y1);
		}
		//g2d.drawOval(0, 0, width, height);
		g2d.dispose();
		
		int owidth=(int)(width/scale);
		int oheight=(int)(height/scale);
		BufferedImage output=new BufferedImage(owidth,oheight,BufferedImage.TYPE_INT_ARGB);
		if(!aa){
			for(int i=0;i<owidth;i++)
				for(int j=0;j<oheight;j++){
					output.setRGB(i, j, input.getRGB((int)(i*scale), (int)(j*scale)));
			}
		}
		else{
			/*Image scaled=input.getScaledInstance(owidth, oheight, Image.SCALE_SMOOTH);
			output.getGraphics().drawImage(scaled, 0, 0, null);*/
			for(int i=0;i<owidth;i++)
				for(int j=0;j<oheight;j++){
					long sumR=0,sumG=0,sumB=0;
					int si=(int)(i*scale),sj=(int)(j*scale);
					int count=0;
					for(int m=si-1;m<=si+1;m++)
						for(int n=sj-1;n<=sj+1;n++){
							if(m>=0 && m<512 && n>=0 && n<512){
								Color color=new Color(input.getRGB(m, n));
								sumR+=color.getRed();
								sumG+=color.getGreen();
								sumB+=color.getBlue();
								count++;
							}
					}
					int pix = 0xff000000 | (((byte)(sumR/count) & 0xff) << 16) | (((byte)(sumG/count) & 0xff) << 8) | ((byte)(sumB/count) & 0xff);
					output.setRGB(i, j, pix);
			}
		}
	    // Use a panel and label to display the image
	    JPanel  panel = new JPanel ();
	    panel.add (new JLabel (new ImageIcon (input)));
	    panel.add (new JLabel (new ImageIcon (output)));
	    
	    JFrame frame = new JFrame("Display images");
	    
	    frame.getContentPane().add (panel);
	    frame.pack();
	    frame.setVisible(true);
	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);  
	}

}
