
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;


public class MyProgram {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage input;
	BufferedImage output;
	int[][] qtable={
			{16,11,10,16,24,40,51,61},
			{12,12,14,19,26,58,60,55},
			{14,13,16,24,40,57,69,56},
			{14,17,22,29,51,87,80,62},
			{18,22,37,56,68,109,103,77},
			{24,35,55,64,81,104,113,92},
			{49,64,78,87,103,121,120,101},
			{72,92,95,98,112,100,103,99}
			};
	int width=352;
	int height=288;
	int N=1;
	public void showIms(String[] args){
		//int width = Integer.parseInt(args[1]);
		//int height = Integer.parseInt(args[2]);
		N = Integer.parseInt(args[1]);
		input = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		try {
			File file = new File(args[0]);
			InputStream is = new FileInputStream(file);

			long len = file.length();
			byte[] bytes = new byte[(int)len];

			int offset = 0;
			int numRead = 0;
			while (offset < bytes.length && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
				offset += numRead;
			}
			is.close();

			float[][] ych=new float[width][height];
			float[][] pbch=new float[width][height];
			float[][] prch=new float[width][height];
			
			int ind = 0;
			for(int y = 0; y < height; y++){
				for(int x = 0; x < width; x++){

					//byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 
					
					ych[x][y]=(float) (0.299*(r&0xFF)+0.587*(g&0xFF)+0.114*(b&0xFF));
					pbch[x][y]=(float) (-0.169*(r&0xFF)-0.331*(g&0xFF)+0.5*(b&0xFF));
					prch[x][y]=(float) (0.5*(r&0xFF)-0.419*(g&0xFF)-0.081*(b&0xFF));
					
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					input.setRGB(x,y,pix);
					ind++;
				}
			}
			float[][] ydct=new float[width][height];
			float[][] pbdct=new float[width][height];
			float[][] prdct=new float[width][height];
			
			for(int i=0;i<width;i+=8)
				for(int j=0;j<height;j+=8){
					for(int u=0;u<N && u<8;u++)
						for(int v=0;u+v<N && v<8;v++){
							float Cu=0,Cv=0;
							Cu = (float) (u==0? 1.0/Math.sqrt(2):1);
							Cv = (float) (v==0? 1.0/Math.sqrt(2):1);
							float yFuv=0,pbFuv=0,prFuv=0;
							for(int x=0;x<8;x++)
								for(int y=0;y<8;y++){
									yFuv+=ych[i+x][j+y]*Math.cos((2*x+1)*u*Math.PI/16)*Math.cos((2*y+1)*v*Math.PI/16);
									pbFuv+=pbch[i+x][j+y]*Math.cos((2*x+1)*u*Math.PI/16)*Math.cos((2*y+1)*v*Math.PI/16);
									prFuv+=prch[i+x][j+y]*Math.cos((2*x+1)*u*Math.PI/16)*Math.cos((2*y+1)*v*Math.PI/16);
							}
							yFuv*=1.0/4.0*Cu*Cv;
							pbFuv*=1.0/4.0*Cu*Cv;
							prFuv*=1.0/4.0*Cu*Cv;
							ydct[i+u][j+v]=(yFuv/qtable[u][v]);
							pbdct[i+u][j+v]=(pbFuv/qtable[u][v]);
							prdct[i+u][j+v]=(prFuv/qtable[u][v]);
					}
			}
			ych=new float[width][height];
			pbch=new float[width][height];
			prch=new float[width][height];
			for(int i=0;i<width;i+=8)
				for(int j=0;j<height;j+=8){
					for(int x=0;x<8;x++)
						for(int y=0;y<8;y++){
							float yfxy=0,pbfxy=0,prfxy=0;
							for(int u=0;u<N && u<8;u++)
								for(int v=0;u+v<N && v<8;v++){
									float Cu=0,Cv=0;
									Cu = (float) (u==0? 1.0/Math.sqrt(2):1);
									Cv = (float) (v==0? 1.0/Math.sqrt(2):1);
									yfxy+=qtable[u][v]*Cu*Cv*ydct[i+u][j+v]*Math.cos((2*x+1)*u*Math.PI/16)*Math.cos((2*y+1)*v*Math.PI/16);
									pbfxy+=qtable[u][v]*Cu*Cv*pbdct[i+u][j+v]*Math.cos((2*x+1)*u*Math.PI/16)*Math.cos((2*y+1)*v*Math.PI/16);
									prfxy+=qtable[u][v]*Cu*Cv*prdct[i+u][j+v]*Math.cos((2*x+1)*u*Math.PI/16)*Math.cos((2*y+1)*v*Math.PI/16);
							}
							ych[i+x][j+y]=yfxy/4;
							pbch[i+x][j+y]=pbfxy/4;
							prch[i+x][j+y]=prfxy/4;
					}
			}
			output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			for(int y = 0; y < height; y++)
				for(int x = 0; x < width; x++){
					int tmpr=(int) (ych[x][y]+1.402*prch[x][y]);
					int tmpg=(int) (ych[x][y]-0.344*pbch[x][y]-0.714*prch[x][y]);
					int tmpb=(int) (ych[x][y]+1.772*pbch[x][y]);
					byte r=(byte) tmpr;
					byte g=(byte) tmpg;
					byte b=(byte) tmpb;
					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					output.setRGB(x,y,pix);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Use labels to display the images
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		JLabel lbText1 = new JLabel("Original image (Left)");
		lbText1.setHorizontalAlignment(SwingConstants.CENTER);
		JLabel lbText2 = new JLabel("Image after modification (Right)");
		lbText2.setHorizontalAlignment(SwingConstants.CENTER);
		lbIm1 = new JLabel(new ImageIcon(input));
		lbIm2 = new JLabel(new ImageIcon(output));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		frame.getContentPane().add(lbText1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 1;
		c.gridy = 0;
		frame.getContentPane().add(lbText2, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
	}

	public static void main(String[] args) {
		MyProgram ren = new MyProgram();
		ren.showIms(args);
	}

}