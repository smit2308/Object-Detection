
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ChartFrame;



public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;
	int width = 640; // default image width and height
	int height = 480;

 
	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
	{
		try
		{
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

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
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
	}


public void showIms(String[] args) {
    // Load the main image and calculate its histogram
    BufferedImage imgMain = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    readImageRGB(width, height, args[0], imgMain);
    BufferedImage imgMainHSV = convertRGBtoHSV(imgMain);
    double[] mainHistogram = plotHueHistogram(imgMainHSV);

    // Define a threshold for matching hues
    double hueThreshold = 100; // Adjust this value as needed

    // Iterate through object images and locate objects in the main image
    for (int i = 1; i < args.length; i++) {
        // Load the object image and calculate its histogram
        BufferedImage imgObject = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[i], imgObject);
        BufferedImage imgObjectHSV = convertRGBtoHSV(imgObject);
        imgObjectHSV = removeGreenBackground(imgObjectHSV);
        double[] objectHistogram = plotHueHistogram(imgObjectHSV);
        displayimage(i, imgObjectHSV);

        // Compare histograms and detect objects
        List<Rectangle> detectedObjects = detectObjects(mainHistogram, objectHistogram);

        // Draw bounding boxes around detected objects
        for (Rectangle rect : detectedObjects) {
            Graphics2D g2d = imgMain.createGraphics();
            g2d.setColor(Color.RED);
            g2d.draw(rect);
            g2d.dispose();
        }
    }
    int x = mainHistogram.length;
    System.out.println(x);

    displayimage(0, imgMain);


        
}

private List<Rectangle> detectObjects(double[] mainHistogram, double[] objectHistogram) {
    List<Rectangle> detectedObjects = new ArrayList<>();

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;
    
    double hueThreshold =1;
    // Iterate through the bins in the histogram
    for (int bin = 0; bin < objectHistogram.length; bin++) {
        // Check if the bin frequency in the object histogram is greater than 0
        if (objectHistogram[bin] > 10) {
            // Compare the bin values using a threshold
            
            double binDiff = Math.abs(mainHistogram[bin] - objectHistogram[bin]);

            // If the difference is below the threshold, consider it a match
            if (binDiff <= hueThreshold) {
                
                // Calculate the x and y coordinates corresponding to this bin
                int x = bin % width;
                int y = bin / width;

                // Update the bounding box coordinates
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
    }

    // Create a single bounding box around all detected regions
    if (minX <= maxX && minY <= maxY) {
        Rectangle objectBoundingBox = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        detectedObjects.add(objectBoundingBox);
    }

    return detectedObjects;
}


    
private double[] plotHueHistogram(BufferedImage hsvImage) {
    int width = hsvImage.getWidth();
    int height = hsvImage.getHeight();

    // Create an array to store the hue values
    double[] hueValues = new double[width * height];


    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // Get the color of the current pixel in the HSV image
            Color hsvColor = new Color(hsvImage.getRGB(x, y));

            // Extract the hue component (scaled to [0, 360])
            float[] hsv = new float[3];
            Color.RGBtoHSB(hsvColor.getRed(), hsvColor.getGreen(), hsvColor.getBlue(), hsv);
            double hue = hsv[0] * 360.0; // Convert hue to [0, 360] scale

            // Store the hue value in the array
            hueValues[y * width + x] = hue;
        }
    }
    

    // Create a histogram dataset
    HistogramDataset dataset = new HistogramDataset();
    dataset.addSeries("Hue", hueValues, 360); // 360 bins for 0-360 degrees


    // Create a chart and plot
    JFreeChart chart = ChartFactory.createHistogram(
            "Hue Histogram",
            "Hue (Degrees)",
            "Frequency",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
    );

    // Customize the chart
    XYPlot plot = (XYPlot) chart.getPlot();
    plot.setDomainPannable(true);
    plot.setRangePannable(true);
    
    // Use an XYLineAndShapeRenderer to display a smooth curve
    XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
    plot.setRenderer(renderer);

    // Create a chart frame to display the histogram
    ChartFrame frame = new ChartFrame("Hue Histogram", chart);
    frame.pack();
    frame.setVisible(true);

    return hueValues;
}

public static BufferedImage removeGreenBackground(BufferedImage inputImage) {
        int width = inputImage.getWidth();
        int height = inputImage.getHeight();
        BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        float[] greenHSV = new float[3];
        Color.RGBtoHSB(0, 255, 0, greenHSV); // Convert pure green to HSV

        // Define a hue threshold to identify green pixels
        float hueThreshold = greenHSV[0] * 360; // Convert hue to [0, 360] scale

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
                }
            }
        }

        return outputImage;
    }

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

  

        public static BufferedImage convertRGBtoHSV(BufferedImage inputImage) {
            int width = inputImage.getWidth();
            int height = inputImage.getHeight();
            BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    // Get the color of the current pixel in the input image
                    Color rgbColor = new Color(inputImage.getRGB(x, y));
    
                    // Convert the RGB color to HSV
                    float[] hsv = new float[3];
                    Color.RGBtoHSB(rgbColor.getRed(), rgbColor.getGreen(), rgbColor.getBlue(), hsv);
    
                    // Create a new color in the output image using the HSV values
                    int newRGB = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
    
                    // Set the new color in the output image
                    outputImage.setRGB(x, y, newRGB);
                }
            }
    
            return outputImage;
        }
 
       /**
     * Display a histogram for the Hue (H) component of an HSV image.
     */
 

	public static void main(String[] args) {
		String[] x = {"dataset\\dataset\\data_sample_rgb\\Apple_image.rgb", "dataset\\dataset\\data_sample_rgb\\Apple_object.rgb"};

		ImageDisplay ren = new ImageDisplay();
		ren.showIms(x);
	}

}
