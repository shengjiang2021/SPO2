/*
 * (The MIT license)
 * 
 * Copyright (c) 2012 MIPT (mr.santak@gmail.com)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
 * and associated documentation files (the "Software"), to deal in the Software without restriction, 
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
 * subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies 
 * or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE 
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ru.mipt.edf;

import ru.mipt.rml.RMLParser;

import java.io.*;
import java.text.MessageFormat;
import java.util.List;
import java.util.Scanner;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class EDF
{
	private static EDFParserResult result = null;
	private static final int SPO2_CHANNEL = 20;

	public static void main(String... args) throws IOException, ClassNotFoundException, InstantiationException,
			IllegalAccessException, UnsupportedLookAndFeelException
	{
		File file;
		if (args.length == 0)
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			JFileChooser fileChooser = new JFileChooser();
			if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION)
				file = fileChooser.getSelectedFile();
			else
				return;
		} else
			file = new File(args[0]);

		System.out.println("Start processing file: " + file.getName());

		new File(file.getParent() + "/data").getAbsoluteFile().mkdir();

		InputStream is = null;
		FileOutputStream fos = null;
		InputStream format = null;
		try
		{
			is = new FileInputStream(file);
			fos = new FileOutputStream(file.getParent() + "/" + file.getName().replaceAll("[.].*", "_header.txt"));
			format = EDFParser.class.getResourceAsStream("header.format");
			result = EDFParser.parseEDF(is);
			writeHeaderData(fos, getPattern(format));
		} finally
		{
			close(is);
			close(fos);
			close(format);
		}
		String channelFormat = null;
		format = null;

		new File(file.getParent() + "/channel").getAbsoluteFile().mkdir();

		try
		{
			format = EDFParser.class.getResourceAsStream("channel_info.format");
			channelFormat = getPattern(format);
		} finally
		{
			close(format);
		}

		for (int i = 0; i < result.getHeader().getNumberOfChannels(); i++)
		{
			try
			{
				if (i == SPO2_CHANNEL) {
					fos = new FileOutputStream(file.getParent() + "/channel/"
							+ file.getName().replaceAll("[.].*", "_channel_info_" + i + ".txt"));
					writeChannelData(fos, channelFormat, i);
				}
			} finally
			{
				close(fos);
			}

			if (i == SPO2_CHANNEL) {
				String spo2ChannelInfoFile = file.getParent() + "/channel/"
						+ file.getName().replaceAll("[.].*", "_channel_info_" + i + ".txt");
				if (isSpo2Channel(spo2ChannelInfoFile)) {
					System.out.println("Finish parsing spo2 channel info");
				} else {
					System.out.println("error: channel 20 is not the spo2");
				}
			}

			try
			{
				if (i == SPO2_CHANNEL) {
					fos = new FileOutputStream(file.getParent() + "/data/"
							+ file.getName().replaceAll("[.].*", "_" + i + ".txt"));
					for (int j = 0; j < result.getSignal().getValuesInUnits()[i].length; j++)
						fos.write((result.getSignal().getValuesInUnits()[i][j] + "\n").getBytes("UTF-8"));
				}
			} finally
			{
				close(fos);
			}
		}

		System.out.println("Finish parsing file: " + file.getName());

		RMLParser.process(new String[] {file.getParent() + "/" + file.getName().replaceAll("[.].*", ".rml"),
				file.getParent() + "/data/" + file.getName().replaceAll("[.].*", "_" + SPO2_CHANNEL + ".txt")});

		List<EDFAnnotation> annotations = result.getAnnotations();
		if (annotations == null || annotations.size() == 0)
			return;
		try
		{
			fos = new FileOutputStream(file.getParent() + "/" + file.getName().replaceAll("[.].*", "_annotation.txt"));
			for (EDFAnnotation annotation : annotations)
			{
				if (annotation.getAnnotations().size() != 0)
				{
					StringBuffer buffer = new StringBuffer();
					buffer.append(annotation.getOnSet()).append(";").append(annotation.getDuration());
					for (int i = 0; i < annotation.getAnnotations().size(); i++)
						buffer.append(";").append(annotation.getAnnotations().get(i));
					buffer.append("\n");
					fos.write(buffer.toString().getBytes());
				}
			}
		} finally
		{
			close(fos);
		}
	}

	private static void writeHeaderData(OutputStream os, String pattern) throws IOException
	{
		String message = MessageFormat.format(pattern, result.getHeader().getIdCode().trim(), result.getHeader()
				.getSubjectID().trim(), result.getHeader().getRecordingID().trim(), result.getHeader().getStartDate()
				.trim(), result.getHeader().getStartTime().trim(), result.getHeader().getBytesInHeader(), result
				.getHeader().getFormatVersion().trim(), result.getHeader().getNumberOfRecords(), result.getHeader()
				.getDurationOfRecords(), result.getHeader().getNumberOfChannels());
		os.write(message.getBytes("UTF-8"));
	}

	private static void writeChannelData(OutputStream os, String pattern, int i) throws IOException
	{
		String message = MessageFormat.format(pattern, result.getHeader().getChannelLabels()[i].trim(), result
				.getHeader().getTransducerTypes()[i].trim(), result.getHeader().getDimensions()[i].trim(), result
				.getHeader().getMinInUnits()[i], result.getHeader().getMaxInUnits()[i], result.getHeader()
				.getDigitalMin()[i], result.getHeader().getDigitalMax()[i], result.getHeader().getPrefilterings()[i]
				.trim(), result.getHeader().getNumberOfSamples()[i], new String(result.getHeader().getReserveds()[i])
				.trim());
		os.write(message.getBytes("UTF-8"));
	}

	private static String getPattern(InputStream is)
	{
		StringBuilder str = new StringBuilder();
		Scanner scn = null;
		try
		{
			scn = new Scanner(is);
			while (scn.hasNextLine())
				str.append(scn.nextLine()).append("\n");
		} finally
		{
			close(scn);
		}
		return str.toString();
	}

	private static final void close(Closeable c)
	{
		try
		{
			c.close();
		} catch (Exception e)
		{
			// do nothing
		}
	}

	private static boolean isSpo2Channel(String file) {
		String[] info = new String[]{};
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String channelName = br.readLine();
			info = channelName.split(":");

		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
		return "SpO2".equals(info[info.length - 1].trim());
	}
}
