import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.LinkedList;
import java.util.Map;

import java.util.Queue;

import javax.swing.*;

// import org.jfree.chart.ChartFactory;
// import org.jfree.chart.ChartPanel;
// import org.jfree.chart.JFreeChart;
// import org.jfree.chart.plot.PlotOrientation;
// import org.jfree.chart.plot.PlotRenderingInfo;
// import org.jfree.chart.plot.XYPlot;
// import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
// import org.jfree.chart.renderer.xy.XYSplineRenderer;
// import org.jfree.data.statistics.HistogramDataset;
// import org.jfree.data.xy.XYSeries;
// import org.jfree.data.xy.XYSeriesCollection;
// import org.jfree.chart.ChartFrame;

public class ObjectDetectionApp{

    public static void main(String[] args) {
		String[] imagePaths = {
            "multi_object_test_new/multi_object_test_new/update_rgb/Oswald_and_Volleyball_v2.rgb",
            "multi_object_test_new/dataset/dataset/data_sample_rgb/Oswald_object.rgb",
            "multi_object_test_new/dataset/dataset/data_sample_rgb/Volleyball_object.rgb"
        };

        ObjectDetectionApp objectDetection = new ObjectDetectionApp();
        objectDetection.showIms(args);
        // obj.showIms(args);
    
}

	JFrame frame;
	JLabel lbIm1;
	JLayeredPane layer;
    BufferedImage imgOne;
    int height=480;
    int width=640;
	BufferedImage imgmain;
	HashMap<String, ArrayList<ArrayList<Integer>>> allcoordinates = new HashMap<>();
	ArrayList<String> objectNames = new ArrayList<>();

   private BufferedImage readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{	
			height = img.getHeight();
			width = img.getWidth();

			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);
			

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					// int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					int pix = ((a << 24) + (r << 16) + (g << 8) + b);

					img.setRGB(x,y,pix);
					ind++;
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		return img;
	}
	private static class ImagePanel extends JPanel {
        private BufferedImage image;
        public ImagePanel(int width, int height, BufferedImage image) {
            this.image = image;
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
            repaint();
        }
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
    }
	public int peak(int[] hist){
		int maxFreq = 0;
			int peakvalue=0;
			for(int i=0;i<hist.length;i++){
				if (maxFreq < hist[i]){
					maxFreq=hist[i];
				}
			}
			for(int i=0;i<hist.length;i++){
				if(hist[i]==maxFreq){
					peakvalue=i;
				}
			}
		return peakvalue;
	}
	
    public void showIms(String[] args){
        //reading original image
		ArrayList<int[]> object_histogram_info = new ArrayList<int[]>();
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgmain = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		

		BufferedImage input = readImageRGB(width, height, args[0], imgmain);
		int[] hueHistogram_input = new int[360];
		
		for(int i=1;i<args.length;i++){
				HashMap<Integer, ArrayList<ArrayList<Integer>>> Map = new HashMap<>();
				BufferedImage imgObject = readImageRGB(width, height, args[i], imgOne);
				int[] objectHistogram = new int[4];
				boolean redf = false;
				objectHistogram= PlotHueHistogram(imgObject,i);
				object_histogram_info.add(objectHistogram);
				if(objectHistogram[1]>=12000){
					redf=true;
				}

				int w=640;
				int h=480;
				int[][] kernel = new int[h][w];
				for(int r=0;r<h;r++){
					for(int c=0;c<w;c++){
						kernel[r][c]=0;
					}
				}

				//<-----MAIN LOGIC FOR COLOR MATCHING IN INPUT IMAGE----->
				  
			for(int y=0;y<height;y++){
			for(int x = 0; x < width; x++){
				int rgb = input.getRGB(x, y);
					int red = rgb >> 16 & 0xFF; 
					int green = (rgb >> 8) & 0xFF;
					int blue = rgb & 0xFF;
				float[] hsv = Color.RGBtoHSB(red,green,blue, null);


				
					int hue = Math.round(hsv[0] * 359);

					
						if(redf){
							if((hue>=0 && hue<=10) || (hue>=335 && hue<=359)){
								kernel[y][x]=1;
	
							}
						}
						else if ((hue>=(object_histogram_info.get(i-1))[3]-10) && (hue<=(object_histogram_info.get(i-1))[3]+10))
						{	
						int[] temp = {x,y};
						kernel[y][x]=1;
		
						}
					}
				}

				Map = NumberofIslandsClustering(kernel,Map);
				SelectCoordinates(args[i],Map,redf);


		}


		
			
		for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
			int rgb = input.getRGB(x, y);
					int red = rgb >> 16 & 0xFF; 
					int green = (rgb >> 8) & 0xFF;
					int blue = rgb & 0xFF;
				float[] hsv = Color.RGBtoHSB(red,green,blue, null);
					int hue = Math.round(hsv[0] * 359);

					hueHistogram_input[hue]++;
				}
			}
	
			
		JFrame frame = new JFrame();
        int width = 640;
        int height = 480;
			
		for (Map.Entry<String, ArrayList<ArrayList<Integer>>> set :
             allcoordinates.entrySet()) {

			for(int j = 0;j<set.getValue().size();j++){
				int x1 = set.getValue().get(j).get(0);				
				int x2 = set.getValue().get(j).get(1);				
				int y1 = set.getValue().get(j).get(2);				
				int y2 = set.getValue().get(j).get(3);
	
				Graphics2D g = input.createGraphics();
				g.setStroke(new java.awt.BasicStroke(3));
				g.setColor(Color.RED);
				g.setFont(new Font("Arial", Font.BOLD,14));
				g.drawString(""+extractObjectName(set.getKey()), x1, y2);
		
				int boxWidth=x2-x1;
				int boxHeight=y2-y1;
				g.drawRect(x1, y1, boxWidth, boxHeight);
				g.dispose();	
			}
		}
        frame.setSize(width, height);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(new ImagePanel(width, height, input));
        frame.setVisible(true);

		

    }

	public static BufferedImage removeGreenBackgroundAndCrop(BufferedImage inputImage) {
		int width = inputImage.getWidth();
		int height = inputImage.getHeight();
		BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		float[] greenHSV = new float[3];
		Color.RGBtoHSB(0, 255, 0, greenHSV);
	
		// Define a hue threshold to identify green pixels
		float hueThreshold = greenHSV[0] * 360; 
	
		int minX = width; 
		int minY = height;
		int maxX = 0;      // Initialize maxX and maxY to minimum values
		int maxY = 0;
	
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				// Get the color of the current pixel in the input image
				Color rgbColor = new Color(inputImage.getRGB(x, y));
	
				// Convert the RGB color to HSV
				float[] hsv = new float[3];
				Color.RGBtoHSB(rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue(), hsv);
	
				// Check if the hue is not within the green range
				if (Math.abs(hsv[0] * 360 - hueThreshold) > 30) {
					// If not green, copy the pixel to the output image
					outputImage.setRGB(x, y, rgbColor.getRGB());
	
					// Update the cropping boundaries
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
	
		// Crop the output image to the boundaries of the colored object
		int croppedWidth = maxX - minX + 1;
		int croppedHeight = maxY - minY + 1;
		BufferedImage croppedImage = new BufferedImage(croppedWidth, croppedHeight, BufferedImage.TYPE_INT_RGB);
	
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				Color rgbColor = new Color(outputImage.getRGB(x, y));
				croppedImage.setRGB(x - minX, y - minY, rgbColor.getRGB());
			}
		}
	
		return scaleToFrameSize(croppedImage, 640, 480);
	}
	
	// To reduce black frequency
	public static BufferedImage scaleToFrameSize(BufferedImage inputImage, int frameWidth, int frameHeight) {
		
		BufferedImage scaledImage = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = scaledImage.createGraphics();
	
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.drawImage(inputImage, 0, 0, frameWidth, frameHeight, null);
		g2d.dispose();
	
		return scaledImage;
	}
	public static String extractObjectName(String input) {
    
		int lastBackslashIndex = input.lastIndexOf("/");
	 
		int dotRgbIndex = input.indexOf(".rgb");
	
		if (lastBackslashIndex != -1 && dotRgbIndex != -1) {
			String objectName = input.substring(lastBackslashIndex + 1, dotRgbIndex);
			return objectName;
		} else {
			// Return an empty string or handle the case where "\\" or ".rgb" is not found
			return "";
		}
	}

	public void SelectCoordinates(String objName,HashMap<Integer, ArrayList<ArrayList<Integer>>> Map,boolean redf){
		
		for (Map.Entry<Integer, ArrayList<ArrayList<Integer>>> set :
             Map.entrySet()) {
 
			if(set.getValue().get(0).size()>=2200){
				int minx = Collections.min(set.getValue().get(1));
				int maxx = Collections.max(set.getValue().get(1));
				int miny = Collections.min(set.getValue().get(0));
				int maxy = Collections.max(set.getValue().get(0));
				ArrayList<Integer> dir = new ArrayList<>();
				dir.add(minx);
				dir.add(maxx);
				dir.add(miny);
				dir.add(maxy);
				if(!allcoordinates.containsKey(objName)){
					allcoordinates.put(objName, new ArrayList<ArrayList<Integer>>());
					allcoordinates.get(objName).add(dir);
				}else{
					allcoordinates.get(objName).add(dir);
				}
				
			}


        }
	
	}

	
	public HashMap<Integer, ArrayList<ArrayList<Integer>>> NumberofIslandsClustering(int[][] grid,HashMap<Integer, ArrayList<ArrayList<Integer>>> Map) {
        int count = 0;
        int[] index = new int[2];
        int row,col;
        for(int i=0;i<grid.length;i++){
            for(int j=0;j<grid[0].length;j++){
                if(grid[i][j] == 1){
                    count++;
                    Queue<int[]> q = new LinkedList<>();
                    q.offer(new int[]{i,j});
                    while(!q.isEmpty()){
                        index = q.poll();
                        row = index[0]; col = index[1];
                        if(row < 0 || row >= grid.length || col < 0 || col >= grid[0].length || grid[row][col] != 1) continue;
                        else {
                            grid[row][col] = 0;
							if(!Map.containsKey(count+2)){
								Map.put(count+2, new ArrayList<ArrayList<Integer>>());
								ArrayList<Integer> x = new ArrayList<>();
								ArrayList<Integer> y = new ArrayList<>();
								Map.get(count+2).add(x);
								Map.get(count+2).add(y);
								Map.get(count+2).get(0).add(row);
								Map.get(count+2).get(1).add(col);
							}else{
								Map.get(count+2).get(0).add(row);
								Map.get(count+2).get(1).add(col);
							}  
                            q.offer(new int[]{row+1,col});
                            q.offer(new int[]{row-1,col});
                            q.offer(new int[]{row,col+1});
                            q.offer(new int[]{row,col-1});
                        }                      
                
                    }
                }
            }
        }
        return Map;
    }
	
	// private List<Rectangle> detectObjects(double[] mainHistogram, double[] objectHistogram, BufferedImage mainImage) {
//     List<Rectangle> detectedObjects = new ArrayList<>();

//     int minX = Integer.MAX_VALUE;
//     int minY = Integer.MAX_VALUE;
//     int maxX = Integer.MIN_VALUE;
//     int maxY = Integer.MIN_VALUE;

//     // Find the bin with the highest value in the object histogram (the peak)
//     int maxBinIndex = -1;
//     double maxBinValue = Double.MIN_VALUE;
//     double start = 15*360/width;
//     int i=0;
//     while(i<=2){
//     for (int bin = (int)start; bin < objectHistogram.length; bin++) {
//         if (objectHistogram[bin] > maxBinValue) {
//             maxBinValue = objectHistogram[bin];
//             maxBinIndex = bin;
//         }
//     }
//     double peakHueDegrees = (maxBinIndex * 360.0) / objectHistogram.length;

//     // Define the hue range threshold in ±10 degrees)
//     double hueRangeThreshold = 2*360/width;
   

//     // Calculate the minimum and maximum hue values based on the peak and threshold
//     double minHueDegrees = peakHueDegrees - hueRangeThreshold;
//     double maxHueDegrees = peakHueDegrees + hueRangeThreshold;

//     for (int y = 0; y < mainImage.getHeight(); y++) {
//         for (int x = 0; x < mainImage.getWidth(); x++) {
//             Color mainColor = new Color(mainImage.getRGB(x, y));
//             float[] mainHSV = new float[3];
//             Color.RGBtoHSB(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), mainHSV);
//             double mainHueDegrees = mainHSV[0] * 360.0;
            

//             // Check if the main image pixel's hue is within the chosen ranges
//             if (mainHueDegrees >= minHueDegrees && mainHueDegrees <= maxHueDegrees) {
//                 // Highlight the pixel
//                 mainImage.setRGB(x, y, Color.RED.getRGB());

//                 // Update the bounding box coordinates
//                 minX = Math.min(minX, x);
//                 minY = Math.min(minY, y);
//                 maxX = Math.max(maxX, x);
//                 maxY = Math.max(maxY, y);
//             }
//         }
//     }

//     start = maxBinIndex+10*360/width;
//     i+=1;
// }

//     // Create a single bounding box around all detected regions
//     if (minX <= maxX && minY <= maxY) {
//         Rectangle objectBoundingBox = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
//         detectedObjects.add(objectBoundingBox);
//     }

//     return detectedObjects;
// }

    

private void displayimage(int i,BufferedImage img)
{
				   // Display the main image with bounding box
			frame = new JFrame();
			frame.setTitle("Object " + i);
			GridBagLayout gLayout = new GridBagLayout();
			frame.getContentPane().setLayout(gLayout);

			lbIm1 = new JLabel(new ImageIcon(img));

			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.CENTER;
			c.weightx = 0.5;
			c.gridx = 0;
			c.gridy = 0;
			
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 0;
			c.gridy = 1;
			frame.getContentPane().add(lbIm1, c);

			frame.pack();
			frame.setVisible(true);
}

public int[] PlotHueHistogram(BufferedImage img,int z){
		int[] imgObjectHistogram=new int[360];
		double[] hsvArray = new double[3];

		for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					int clr=img.getRGB(x, y);
					int blue = clr & 0xff;
					int green = (clr & 0xff00) >> 8;
					int red = (clr & 0xff0000) >> 16;

					int rgb = img.getRGB(x, y);
					hsvArray = RGBToHSV(rgb);

				float[] hsv = Color.RGBtoHSB(red,green,blue, null);
				int hue = (int) (hsv[0]*360);

					if(green==255 && red==0 && blue==0){
						continue;
					}else{
						imgObjectHistogram[hue]++;

					}
				}
			}
			 // // Create a histogram dataset
    // HistogramDataset dataset = new HistogramDataset();
    // dataset.addSeries("Hue", hueValues, 360); // 360 bins for 0-360 degrees

    // // Create a chart and plot
    // JFreeChart chart = ChartFactory.createHistogram(
    //         "Hue Histogram " + i,
    //         "Hue (Degrees)",
    //         "Frequency",
    //         dataset,
    //         PlotOrientation.VERTICAL,
    //         true,
    //         true,
    //         false
    // );

    // XYPlot plot = (XYPlot) chart.getPlot();
    // plot.setDomainPannable(true);
    // plot.setRangePannable(true);
    // XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    // plot.setRenderer(renderer);

    // ChartFrame frame = new ChartFrame("Hue Histogram", chart);
    // frame.pack();
    // frame.setVisible(true);

			int maxHueVal = 0;
			int thatSum=0;
			int redSum=0;
			//LOOP FOR CHECKING RED
			
				for(int i=0;i<15;i++)
				{
					redSum+=imgObjectHistogram[i];
				}
				for(int i=345;i<360;i++){
					redSum+=imgObjectHistogram[i];
				}

				for(int i=45;i<60;i++){
				thatSum=0;
					thatSum+=imgObjectHistogram[i];
				}
				if(thatSum>=4000){
						maxHueVal=60;
					}
					else{
						imgObjectHistogram[60]=0;
						maxHueVal=peak(imgObjectHistogram);
					}
			
			return new int[] {peak(imgObjectHistogram),redSum,0,maxHueVal};
	}
	

// Convert RGB to HSV
    public static double[] RGBToHSV(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

		double h, s, v;

        double min, max, delta;

        min = Math.min(Math.min(r, g), b);
        max = Math.max(Math.max(r, g), b);

        v = max;

        delta = max - min;

        // S
        if (max != 0)
            s = delta / max;
        else {
            s = 0;
            h = -1;
            return new double[] { h, s, v };
        }

        // H
        if (r == max)
            h = (g - b) / delta; // between yellow & magenta
        else if (g == max)
            h = 2 + (b - r) / delta; // between cyan & yellow
        else
            h = 4 + (r - g) / delta; // between magenta & cyan

        h *= 60; // degrees

        if (h < 0)
            h += 360;

        h = h * 1.0;
        s = s * 100.0;
        v = (v / 256.0) * 100.0;

		return new double[] { h, s, v };
		
    }


}