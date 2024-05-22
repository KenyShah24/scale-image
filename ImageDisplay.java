
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.lang.String;
import java.awt.event.*;

public class ImageDisplay extends JPanel {

	JFrame frame;
	JLabel lbIm1, overlay;
	JPanel panel;
	BufferedImage imgOne;
	BufferedImage newImg, outputImg, overlayImg;
	boolean AntiAliasing;
	int width = 7680; // default image width and height
	int height = 4320;
	int newWidth = 0;
	int newHeight = 0;
	private boolean ctrlPressed;
	private int mouseX;
	private int windowSize;
	private int mouseY;
	double ScalingFactor;
	int originalImagePixels[][];
	int scaledImagePixels[][];
	double[][] filter3x3 = {
			{ 1.0 / 16, 1.0 / 8, 1.0 / 16 },
			{ 1.0 / 8, 1.0 / 4, 1.0 / 8 },
			{ 1.0 / 16, 1.0 / 8, 1.0 / 16 }
	};

	/**
	 * Read Image RGB
	 * Reads the image of given width and height at the given imgPath into the
	 * provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
		try {
			int frameLength = width * height * 3;
			originalImagePixels = new int[height][width];
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			int ind = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					// int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					originalImagePixels[y][x] = pix;
					img.setRGB(x, y, pix);
					ind++;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private BufferedImage CloneImage(BufferedImage img) {
		ColorModel model = img.getColorModel();
		boolean alpha = model.isAlphaPremultiplied();
		WritableRaster rasterScan = img.copyData(null);
		return new BufferedImage(model, rasterScan, alpha, null);
	}

	private BufferedImage applyFilter(BufferedImage inputImage, double[][] kernel) {
		int width = inputImage.getWidth();
		int height = inputImage.getHeight();
		BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		int kernelSize = kernel.length;
		int halfKernel = kernelSize / 2;

		for (int y = halfKernel; y < height - halfKernel; y++) {
			for (int x = halfKernel; x < width - halfKernel; x++) {
				double redSum = 0.0;
				double greenSum = 0.0;
				double blueSum = 0.0;

				for (int ky = 0; ky < kernelSize; ky++) {
					for (int kx = 0; kx < kernelSize; kx++) {
						int pixelX = x - halfKernel + kx;
						int pixelY = y - halfKernel + ky;

						int rgb = inputImage.getRGB(pixelX, pixelY);
						int red = (rgb >> 16) & 0xFF;
						int green = (rgb >> 8) & 0xFF;
						int blue = rgb & 0xFF;

						redSum += red * kernel[ky][kx];
						greenSum += green * kernel[ky][kx];
						blueSum += blue * kernel[ky][kx];
					}
				}

				int newRed = (int) Math.round(redSum);
				int newGreen = (int) Math.round(greenSum);
				int newBlue = (int) Math.round(blueSum);

				int newPixel = (newRed << 16) | (newGreen << 8) | newBlue;
				scaledImagePixels[y][x] = newPixel;
				outputImage.setRGB(x, y, newPixel);
			}
		}

		return outputImage;
	}

	private void GetNewImageWidthAndHeight() {
		if (this.ScalingFactor > 0 & this.ScalingFactor < 20) {
			this.newWidth = (int) Math.ceil(this.width * this.ScalingFactor);
			this.newHeight = (int) Math.ceil(this.height * this.ScalingFactor);
			System.out.printf("New Scaled Image Width: %d Height: %d%n", this.newWidth, this.newHeight);
		} else {
			System.out.println(
					"Invalid Scaling Factor since the image will be bigger then the original image or way to small to render");
		}
	}

	private BufferedImage ScaleTheOriginialImageIntoNewImage(BufferedImage inputImage) {
		BufferedImage outputImage = CloneImage(inputImage);
		if ((AntiAliasing == false) && (ScalingFactor == 1.0)) {
			return outputImage;
		}
		if (AntiAliasing) {
			System.out.println("Scaling the image with anti-aliasing (Low pass filter)");
			outputImage = applyFilter(inputImage, filter3x3);
		} else {
			System.out.println("Scaling the image without anti-aliasing");
		}
		return outputImage;
	}

	public void showIms(String[] args) {

		AntiAliasing = Integer.parseInt(args[2]) == 1;
		ScalingFactor = Double.parseDouble(args[1]);
		this.windowSize = Integer.parseInt(args[3]);
		this.ctrlPressed = false;

		GetNewImageWidthAndHeight();
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);
		newImg = downsampleImage(imgOne, ScalingFactor);
		newImg = ScaleTheOriginialImageIntoNewImage(newImg);
		outputImg = CloneImage(newImg);

		// Use label to display the image
		frame = new JFrame();
		Container pane = frame.getContentPane();
		GridBagLayout gLayout = new GridBagLayout();
		pane.setLayout(gLayout);
		lbIm1 = new JLabel(new ImageIcon(newImg));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 1;
		pane.add(lbIm1, c);
		frame.pack();
		frame.setVisible(true);
		frame.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
					ctrlPressed = true;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
					insertOverlay(true);
					frame.repaint();
					ctrlPressed = false;
				}
			}
		});
		frame.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				insertOverlay(true);
				mouseX = e.getX();
				mouseY = e.getY();
				int sourceX = (int) (mouseX / ScalingFactor);
				int sourceY = (int) (mouseY / ScalingFactor);
				if (ctrlPressed) {
					int minX = Math.max(0, mouseX - windowSize / 2);
					int minY = Math.max(0, mouseY - windowSize / 2);
					int maxX = Math.min(newWidth - 1, mouseX + windowSize / 2);
					int maxY = Math.min(newHeight - 1, mouseY + windowSize / 2);
					int startX = Math.max(0, sourceX - windowSize / 2);
					int startY = Math.max(0, sourceY - windowSize / 2);
					int endX = Math.min(imgOne.getWidth() - 1, sourceX + windowSize / 2);
					int endY = Math.min(imgOne.getHeight() - 1, sourceY + windowSize / 2);
					for (int y = minY; y < maxY; y++) {
						int originalX = startX;
						for (int x = minX; x < maxX; x++) {
							if (originalX < endX && startY < endY) {
								int rgb = imgOne.getRGB(originalX, startY);
								originalX++;
								newImg.setRGB(x, y, rgb);
							}
						}
						startY++;
					}
				}
				frame.repaint();
			}
		});

	}

	private void insertOverlay(boolean setOriginalImage) {
		int sourceX = (int) (mouseX / ScalingFactor);
		int sourceY = (int) (mouseY / ScalingFactor);
		if (ctrlPressed) {
			int minX = Math.max(0, mouseX - windowSize / 2);
			int minY = Math.max(0, mouseY - windowSize / 2);
			int maxX = Math.min(newWidth - 1, mouseX + windowSize / 2);
			int maxY = Math.min(newHeight - 1, mouseY + windowSize / 2);
			int startX = Math.max(0, sourceX - windowSize / 2);
			int startY = Math.max(0, sourceY - windowSize / 2);
			int endX = Math.min(imgOne.getWidth() - 1, sourceX + windowSize / 2);
			int endY = Math.min(imgOne.getHeight() - 1, sourceY + windowSize / 2);
			for (int y = minY; y < maxY; y++) {
				int originalX = startX;
				for (int x = minX; x < maxX; x++) {
					if (originalX < endX && startY < endY) {
						int rgb = imgOne.getRGB(originalX, startY);
						originalX++;
						if (!setOriginalImage) {
							newImg.setRGB(x, y, rgb);
						} else {
							newImg.setRGB(x, y, scaledImagePixels[y][x]);
						}
					}
				}
				startY++;
			}
		}
	}

	private BufferedImage downsampleImage(BufferedImage originalImage, double scaleFactor) {
		int originalWidth = originalImage.getWidth();
		int originalHeight = originalImage.getHeight();
		int newWidth = (int) (originalWidth * scaleFactor);
		int newHeight = (int) (originalHeight * scaleFactor);

		BufferedImage downsampledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
		scaledImagePixels = new int[newHeight][newWidth];
		for (int y = 0; y < newHeight; y++) {
			for (int x = 0; x < newWidth; x++) {
				int sourceX = (int) (x / scaleFactor);
				int sourceY = (int) (y / scaleFactor);

				int avgColor = getAverageColor(originalImage, sourceX, sourceY);
				scaledImagePixels[y][x] = avgColor;
				downsampledImage.setRGB(x, y, avgColor);
			}
		}

		return downsampledImage;
	}

	public static int getAverageColor(BufferedImage image, int x, int y) {
		int redSum = 0;
		int greenSum = 0;
		int blueSum = 0;
		int numPixels = 0;

		for (int i = -1; i <= 1; i++) {
			for (int j = -1; j <= 1; j++) {
				int pixelX = x + i;
				int pixelY = y + j;

				if (pixelX >= 0 && pixelX < image.getWidth() && pixelY >= 0 && pixelY < image.getHeight()) {
					int rgb = image.getRGB(pixelX, pixelY);
					redSum += (rgb >> 16) & 0xFF;
					greenSum += (rgb >> 8) & 0xFF;
					blueSum += rgb & 0xFF;
					numPixels++;
				}
			}
		}

		int avgRed = redSum / numPixels;
		int avgGreen = greenSum / numPixels;
		int avgBlue = blueSum / numPixels;

		return (avgRed << 16) | (avgGreen << 8) | avgBlue;
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
