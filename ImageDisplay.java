
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    double[] mainHistogram = plotHueHistogram(imgMainHSV,0);


    Color[] boxColors = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE, Color.PINK};

    // Iterate through object images
    for (int i = 1; i < args.length; i++) {
        
        BufferedImage imgObject = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        readImageRGB(width, height, args[i], imgObject);
        BufferedImage imgObjectHSV = convertRGBtoHSV(imgObject);
        imgObjectHSV = removeGreenBackgroundAndCrop(imgObjectHSV);
        double[] objectHistogram = plotHueHistogram(imgObjectHSV,i);

        
        List<Rectangle> detectedObjects = detectObjects(mainHistogram, objectHistogram, imgMainHSV);

        // Draw bounding boxes around detected objects
        for (Rectangle rect : detectedObjects) {
            Graphics2D g2d = imgMainHSV.createGraphics();
            g2d.setColor(boxColors[i]);
            g2d.setFont(new Font("Arial", Font.BOLD, 14));
        
        g2d.drawString(""+extractObjectName(args[i]), 0, 20);
            g2d.draw(rect);
            g2d.dispose();
        }
    }


    displayimage(0, imgMainHSV);


        
}

public static String extractObjectName(String input) {
    
    int lastBackslashIndex = input.lastIndexOf("\\");
 
    int dotRgbIndex = input.indexOf(".rgb");

    if (lastBackslashIndex != -1 && dotRgbIndex != -1) {
        String objectName = input.substring(lastBackslashIndex + 1, dotRgbIndex);
        return objectName;
    } else {
        // Return an empty string or handle the case where "\\" or ".rgb" is not found
        return "";
    }
}

private List<Rectangle> detectObjects(double[] mainHistogram, double[] objectHistogram, BufferedImage mainImage) {
    List<Rectangle> detectedObjects = new ArrayList<>();

    int minX = Integer.MAX_VALUE;
    int minY = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE;
    int maxY = Integer.MIN_VALUE;

    // Find the bin with the highest value in the object histogram (the peak)
    int maxBinIndex = -1;
    double maxBinValue = Double.MIN_VALUE;
    double start = 10*360/width;
    int i=0;
    while(i<=2){
    for (int bin = (int)start; bin < objectHistogram.length; bin++) {
        if (objectHistogram[bin] > maxBinValue) {
            maxBinValue = objectHistogram[bin];
            maxBinIndex = bin;
        }
    }
    double peakHueDegrees = (maxBinIndex * 360.0) / objectHistogram.length;

    // Define the hue range threshold in ±10 degrees)
    double hueRangeThreshold = 8;
   

    // Calculate the minimum and maximum hue values based on the peak and threshold
    double minHueDegrees = peakHueDegrees - hueRangeThreshold;
    double maxHueDegrees = peakHueDegrees + hueRangeThreshold;

    for (int y = 0; y < mainImage.getHeight(); y++) {
        for (int x = 0; x < mainImage.getWidth(); x++) {
            Color mainColor = new Color(mainImage.getRGB(x, y));
            float[] mainHSV = new float[3];
            Color.RGBtoHSB(mainColor.getRed(), mainColor.getGreen(), mainColor.getBlue(), mainHSV);
            double mainHueDegrees = mainHSV[0] * 360.0;
            

            // Check if the main image pixel's hue is within the chosen ranges
            if (mainHueDegrees >= minHueDegrees && mainHueDegrees <= maxHueDegrees) {
                // Highlight the pixel
                mainImage.setRGB(x, y, Color.RED.getRGB());

                // Update the bounding box coordinates
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
    }

    start = maxBinIndex+10*360/width;
    i+=1;
}

    // Create a single bounding box around all detected regions
    if (minX <= maxX && minY <= maxY) {
        Rectangle objectBoundingBox = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
        detectedObjects.add(objectBoundingBox);
    }

    return detectedObjects;
}

    
private double[] plotHueHistogram(BufferedImage hsvImage, int i) {
    int width = hsvImage.getWidth();
    int height = hsvImage.getHeight();
    double[] hueValues = new double[360];

    for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
            // Get the color of the current pixel in the HSV image
            Color hsvColor = new Color(hsvImage.getRGB(x, y));
            float[] hsv = new float[3];
            Color.RGBtoHSB(hsvColor.getRed(), hsvColor.getGreen(), hsvColor.getBlue(), hsv);
            double hue = hsv[0] * 360.0; // Convert hue to [0, 360] scale
            if (hue >= 0 && hue < 360) {
                // Store the hue value in the array
                hueValues[(int) hue]++;
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

    // // Create a chart frame to display the histogram
    // ChartFrame frame = new ChartFrame("Hue Histogram", chart);
    // frame.pack();
    // frame.setVisible(true);

    return hueValues;
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
 

 

	public static void main(String[] args) {
		// String[] x = {"multi_object_test_new\\multi_object_test_new\\update_rgb\\Multiple_Volleyballs_v2.rgb", "multi_object_test_new/dataset/dataset/data_sample_rgb/Volleyball_object.rgb","multi_object_test_new/dataset/dataset/data_sample_rgb/Oswald_object.rgb"};

		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
