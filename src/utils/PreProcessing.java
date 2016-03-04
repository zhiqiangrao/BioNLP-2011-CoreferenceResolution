package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.logging.Logger;

public class PreProcessing {
	
	private final static Logger logger = Logger.getLogger(PreProcessing.class.getName());

	public static void geniaSS() {
		
		String root = System.getProperty("user.dir");
		//File txtdir = new File(root + "/data/BioNLP-ST_2011_coreference_development_data");
		//File outdir = new File(root + "/data/geniass/devel");
		File txtdir = new File(root + "/data/BioNLP-ST_2011_coreference_training_data");
		File outdir = new File(root + "/data/geniass/train");
		
		if (!outdir.exists())
			outdir.mkdirs();
		
		if (txtdir.isDirectory()) {
			File[] txtfiles = txtdir.listFiles(new FileFilterImpl(".txt"));
			Arrays.sort(txtfiles);
			for (File txtfile : txtfiles) {
				File input = txtfile;
				File output = new File(outdir.getPath() + "/" + FileUtil.removeFileNameExtension(txtfile.getName()) + ".ss");
	
				File inputFolder = new File(root + "/tools/geniass/");
				ProcessBuilder pb = new ProcessBuilder(root + "/tools/geniass/geniass", input.getPath(), output.getPath());
				pb.directory(inputFolder);
				Process process;
				try {
					process = pb.start();
					BufferedReader r = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
					String line;
					while ((line = r.readLine()) != null) {
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	
	private Process enjuProcess;
	private BufferedWriter enjuInputWriter;
	private BufferedReader enjuOutputReader;
	private BufferedReader enjuErrorReader;
	
	public void enjuParse() {
		
		String root = System.getProperty("user.dir");
		File ssdir = new File(root + "/data/geniass/devel");
		File outdir = new File(root + "/data/enju/devel");
		File xmldir = new File(root + "/data/xml/devel");
		//File ssdir = new File(root + "/data/geniass/train");
		//File outdir = new File(root + "/data/enju/train");
		//File xmldir = new File(root + "/data/xml/train");
		
		if (!outdir.exists())
			outdir.mkdirs();
		if (!xmldir.exists())
			xmldir.mkdirs();
		
		Runtime rt = Runtime.getRuntime();
		try {
			System.out.print("Loading Enju....");
			this.enjuProcess = rt.exec("tools/enju/enju -xml -genia");
			OutputStreamWriter outStreamWriter = new OutputStreamWriter(
					this.enjuProcess.getOutputStream());
			this.enjuInputWriter = new BufferedWriter(outStreamWriter);
			InputStreamReader inStreamReader = new InputStreamReader(
					this.enjuProcess.getInputStream());
			this.enjuOutputReader = new BufferedReader(inStreamReader);
			InputStreamReader errorStreamReader = new InputStreamReader(
					this.enjuProcess.getErrorStream());
			this.enjuErrorReader = new BufferedReader(errorStreamReader);
			String line = this.enjuErrorReader.readLine();
			while (!line.equals("Ready")) {
				line = this.enjuErrorReader.readLine();
			}
			System.out.println("Ready!");
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (ssdir.isDirectory()) {
			File[] ssfiles = ssdir.listFiles(new FileFilterImpl(".ss"));
			Arrays.sort(ssfiles);
			int filenum = 0;
			for (File ssfile : ssfiles) {
				File input = ssfile;
				File output = new File(outdir.getPath() + "/" + FileUtil.removeFileNameExtension(ssfile.getName()) + ".ptb");
				File xmlfile = new File(xmldir.getPath() + "/" + FileUtil.removeFileNameExtension(ssfile.getName()) + ".xml");
				logger.info("Extracting from file: " + input.getName() + " " + (++filenum));
				try {
				    BufferedReader br;
					br = new BufferedReader(new FileReader(input));
					StringBuilder sb = new StringBuilder();
					String s;
				    while ((s = br.readLine()) != null) {
				    	this.enjuInputWriter.write(s);
				    	this.enjuInputWriter.newLine();
				    	this.enjuInputWriter.flush();
				    	sb.append(this.enjuOutputReader.readLine());
				    	sb.append("\n");
				    }
				    br.close();
					
				    FileUtil.saveFile(sb.toString(), xmlfile);
				    
				    // xml2ptb
				    FileUtil.saveFile(sb.toString(), new File(root + "/tools/enju/share/enju2ptb/xmlin.xml"));
				    ProcessBuilder pb = new ProcessBuilder(root + "/tools/enju/share/enju2ptb/myconvert");
					Process process = pb.start();
					try {
						Thread.currentThread().sleep(1000);//延时 毫秒 
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String ptbstr = FileUtil.readFile(new File(root + "/tools/enju/share/enju2ptb/ptbout.ptb"));
					ptbstr = ptbstr.replace("(TOP", "(ROOT");
					FileUtil.saveFile(ptbstr, output);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public static void main(String[] args) {
		//geniaSS();
		
		PreProcessing pp = new PreProcessing();
		pp.enjuParse();
	}
	
}
