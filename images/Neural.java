package neural;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

class Window extends JFrame{
	BufferedImage img;
	
	// vykresleni obrazku
	public class Graph extends JPanel{
		@Override
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			Graphics2D g2D = (Graphics2D) g;     
			g.drawImage(img, 0, 0, null);
		}
	}
	
	// okno s obrazkem
	public Window(BufferedImage img){
		this.img = img;
		Graph graph = new Graph();
		graph.setBackground(Color.WHITE);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.add(graph);
		setContentPane(panel);
		this.setSize(600, 400);
		this.setVisible(true);
		graph.repaint();
	}
	
	// vypocte z obrazku histogram 64 barev
	public static double[] getHistogram(BufferedImage img){
		double[] histogram = new double[64];
		Raster image = img.getData();
		int[] color = new int[3];
		int r,g,b;
		double a = 1.0/(img.getWidth()*img.getHeight());
		for(int i=0;i<img.getHeight();i++){
			for(int j=0;j<img.getWidth();j++){
				image.getPixel(j, i, color);
				r = (int)Math.floor((double)color[0]/63.75);
				if(r > 3) r = 3;
				g = (int)Math.floor((double)color[1]/63.75);
				if(g > 3) g = 3;
				b = (int)Math.floor((double)color[2]/63.75);
				if(b > 3) b = 3;
				histogram[r*16+g*4+b]+= a;
			}
		}
		return histogram;
	}
}

// Neuronova sit
class NeuralNetwork{
	final double GAMMA = 1.0;
	private int nLayer1, nLayer2;
	private int nInput, nOutput;
	public double[][] weights1, weights2, weights3;
	private double[][] dWeights1, dWeights2, dWeights3;

	NeuralNetwork(int nLayer1, int nLayer2, int nInput, int nOutput){
		this.nLayer1 = nLayer1;
		this.nLayer2 = nLayer2;
		this.nOutput = nOutput;
		this.nInput = nInput;
		this.weights1 = new double[nInput+1][nLayer1];
		this.weights2 = new double[nLayer1+1][nLayer2];
		this.weights3 = new double[nLayer2+1][nOutput];
		this.dWeights1 = new double[nInput+1][nLayer1];
		this.dWeights2 = new double[nLayer1+1][nLayer2];
		this.dWeights3 = new double[nLayer2+1][nOutput];
	}
	
	private static void plus(double[][] a, double[][] b, double times){
		for(int i=0;i<a.length;i++){
			for(int j=0;j<a[i].length;j++){
				a[i][j]+= times*b[i][j];
			}
		}
	}
	
	void randomizeWeights(){
		for(int i=0;i<nInput+1;i++){
			for(int j=0;j<nLayer1;j++) this.weights1[i][j] = 1.0-2.0*Math.random();
		}
		for(int i=0;i<nLayer1+1;i++){
			for(int j=0;j<nLayer2;j++) this.weights2[i][j] = 1.0-2.0*Math.random();
		}
		for(int i=0;i<nLayer2+1;i++){
			for(int j=0;j<nOutput;j++) this.weights3[i][j] = 1.0-2.0*Math.random();
		}
	}
	
	double[] classify(double[] input){
		double[] outputLayer1 = new double[nLayer1];
		double[] outputLayer2 = new double[nLayer2];
		return classify(input, outputLayer1, outputLayer2);
	}
	
	// klasifikace neuronove site
	double[] classify(double[] input, double[] outputLayer1, double[] outputLayer2){
		double output[] = new double[nOutput];
		for(int i=0;i<nLayer1;i++){
			outputLayer1[i] = weights1[nInput][i];
			for(int j=0;j<nInput;j++) outputLayer1[i]+= input[j]*weights1[j][i];
			outputLayer1[i]*= -GAMMA;
			outputLayer1[i] = 1.0/(1.0+Math.exp(outputLayer1[i]));
		}
		for(int i=0;i<nLayer2;i++){
			outputLayer2[i] = weights2[nLayer1][i];
			for(int j=0;j<nLayer1;j++) outputLayer2[i]+= outputLayer1[j]*weights2[j][i];
			outputLayer2[i]*= -GAMMA;
			outputLayer2[i] = 1.0/(1.0+Math.exp(outputLayer2[i]));
		}
		for(int i=0;i<nOutput;i++){
			output[i] = weights3[nLayer2][i];
			for(int j=0;j<nLayer2;j++) output[i]+= outputLayer2[j]*weights3[j][i];
			output[i]*= -GAMMA;
			output[i] = 1.0/(1.0+Math.exp(output[i]));
		}
		return output;
	}
	
	// uceni neuronove site
	void learnBPROP(double[][] inputData, double[][] outputData, double learnSpeed, double momentum){
		for(int iData=0;iData<inputData.length;iData++){
			double[] input = inputData[iData];
			double[] expectedOutput = outputData[iData];
			double[] outputLayer1 = new double[nLayer1];
			double[] outputLayer2 = new double[nLayer2];
			double[] outputLayer3;
			outputLayer3 = classify(input, outputLayer1, outputLayer2);
			double[] delta = new double[nOutput];
			for(int i=0;i<nOutput;i++){
				delta[i] = GAMMA*(outputLayer3[i]*(1-outputLayer3[i])*(expectedOutput[i]-outputLayer3[i]));
			}
			double[][] dWeights = new double[nLayer2+1][nOutput];
			for(int i=0;i<nLayer2;i++){
				for(int j=0;j<nOutput;j++) dWeights[i][j] = learnSpeed*delta[j]*outputLayer2[i];
			}
			for(int j=0;j<nOutput;j++) dWeights[nLayer2][j] = learnSpeed*delta[j];
			NeuralNetwork.plus(weights3, dWeights, 1.0);
			NeuralNetwork.plus(weights3, dWeights3, momentum);
			dWeights3 = dWeights;
			
			double[] s = new double[nLayer2];
			for(int i=0;i<nLayer2;i++){
				s[i] = 0.0;
				for(int j=0;j<nOutput;j++) s[i]+= delta[j]*weights3[i][j];
			}
			delta = new double[nLayer2];
			for(int i=0;i<nLayer2;i++){
				delta[i] = GAMMA*(outputLayer2[i]*(1-outputLayer2[i])*s[i]);
			}
			dWeights = new double[nLayer1+1][nLayer2];
			for(int i=0;i<nLayer1;i++){
				for(int j=0;j<nLayer2;j++) dWeights[i][j] = learnSpeed*delta[j]*outputLayer1[i];
			}
			for(int j=0;j<nLayer2;j++) dWeights[nLayer1][j] = learnSpeed*delta[j];
			NeuralNetwork.plus(weights2, dWeights, 1.0);
			NeuralNetwork.plus(weights2, dWeights2, momentum);
			dWeights2 = dWeights;
			
			s = new double[nLayer1];
			for(int i=0;i<nLayer1;i++){
				s[i] = 0.0;
				for(int j=0;j<nLayer2;j++) s[i]+= delta[j]*weights2[i][j];
			}
			delta = new double[nLayer1];
			for(int i=0;i<nLayer1;i++){
				delta[i] = GAMMA*(outputLayer1[i]*(1-outputLayer1[i])*s[i]);
			}
			dWeights = new double[nInput+1][nLayer1];
			for(int i=0;i<nInput;i++){
				for(int j=0;j<nLayer1;j++) dWeights[i][j] = learnSpeed*delta[j]*input[i];
			}
			for(int j=0;j<nLayer1;j++) dWeights[nInput][j] = learnSpeed*delta[j];
			NeuralNetwork.plus(weights1, dWeights, 1.0);
			NeuralNetwork.plus(weights1, dWeights1, momentum);
			dWeights1 = dWeights;
		}
	}
}

// Aplikace
public class Neural {
	
	public static void main(String[] args){
		
		BufferedImage img = null;
		
		double[][] classes = new double[40][4];
		double[][] features = new double[40][320];
		// zjistime priznaky u vsech obrazku - sestavime trenovaci sadu
		for(int iImg=0;iImg<40;iImg++){
			try {
				img = ImageIO.read(new File(""+(iImg+1)+".jpg"));
			}catch(IOException e){
				System.err.println("Nelze načíst obrázek "+(iImg+1));
				System.exit(1);
			}
			double[] histLeft = Window.getHistogram(img.getSubimage(0, 0, img.getWidth()/2, img.getHeight()));
			double[] histRight = Window.getHistogram(img.getSubimage(img.getWidth()/2, 0, img.getWidth()/2, img.getHeight()));
			double[] histTop = Window.getHistogram(img.getSubimage(0, 0, img.getWidth(), img.getHeight()/2));
			double[] histBottom = Window.getHistogram(img.getSubimage(0, img.getHeight()/2, img.getWidth(), img.getHeight()/2));
			for(int i=0;i<64;i++) features[iImg][i] = histLeft[i];
			for(int i=64;i<128;i++) features[iImg][i] = histRight[i-64];
			for(int i=128;i<192;i++) features[iImg][i] = histTop[i-128];
			for(int i=192;i<256;i++) features[iImg][i] = histBottom[i-192];
			classes[iImg][iImg/10] = 1.0;
		}
		
		// vytvorime sit, kterou naucime nad trenovacimi daty
		NeuralNetwork neural = new NeuralNetwork(75, 15, 256, 4);
		neural.randomizeWeights();
		for(int i=0;i<5000;i++){
			int idx = (int)Math.floor(Math.random()*40);
			double[][] D = new double[1][256];
			double[][] C = new double[1][4];
			D[0] = features[idx];
			C[0] = classes[idx];
			neural.learnBPROP(D, C, 0.2, 0.8);
			if(i % 100 == 0){
				int cOK = 0;
				for(int j=0;j<40;j++){
					double[] out = neural.classify(features[j]);
					idx = 0;
					double max = out[0];
					for(int k=1;k<out.length;k++){
						if(out[k] > max){
							max = out[k];
							idx = k;
						}
					}
					if(classes[j][idx] == 1){
						cOK++;
					}
				}
				System.out.println("Vysledek: "+(double)cOK/40.0);
			}
		}
		
		
		// Nacteme novy (neznamy) obrazek, ktery nebyl v trenovaci sade a vypocteme jeho priznaky
		try {
			img = ImageIO.read(new File("night.jpg"));
		}catch(IOException e){
			System.err.println("Nelze načíst obrázek");
			System.exit(1);
		}
		double[] imgFeatures = new double[256];
		double[] histLeft = Window.getHistogram(img.getSubimage(0, 0, img.getWidth()/2, img.getHeight()));
		double[] histRight = Window.getHistogram(img.getSubimage(img.getWidth()/2, 0, img.getWidth()/2, img.getHeight()));
		double[] histTop = Window.getHistogram(img.getSubimage(0, 0, img.getWidth(), img.getHeight()/2));
		double[] histBottom = Window.getHistogram(img.getSubimage(0, img.getHeight()/2, img.getWidth(), img.getHeight()/2));
		for(int i=0;i<64;i++) imgFeatures[i] = histLeft[i];
		for(int i=64;i<128;i++) imgFeatures[i] = histRight[i-64];
		for(int i=128;i<192;i++) imgFeatures[i] = histTop[i-128];
		for(int i=192;i<256;i++) imgFeatures[i] = histBottom[i-192];
		
		// provedeme klasifikaci		
		double[] out = neural.classify(imgFeatures);
		int idx = 0;
		double max = out[0];
		for(int k=1;k<out.length;k++){
			if(out[k] > max){
				max = out[k];
				idx = k;
			}
		}
		
		String vysledek = "";
		if(idx == 0) vysledek = "Result: KRAJINA";
		else if(idx == 1) vysledek = "Result: SKEN";
		else if(idx == 2) vysledek = "Result: NOCNI SCENA";
		else if(idx == 3) vysledek = "Result: INTERIER";
		System.out.println(vysledek);
		JOptionPane.showMessageDialog(null, vysledek);
		
		Window window = new Window(img);
		
	}
}
