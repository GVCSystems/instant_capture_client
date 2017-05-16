package com.arpit;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.sun.org.apache.bcel.internal.generic.NEW;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;


public class NewEncoder extends JFrame {
    Webcam webcam;
    IMediaWriter writer;
    public int clicks = 0;
    Dimension size = WebcamResolution.QVGA.getSize();

    Logger logger = LoggerFactory.getLogger(NewEncoder.class);

    ArrayList<IVideoPicture> arrayList = new ArrayList<IVideoPicture>();

    public String cam_number = "CAM01";
    public int frame_buffer = 100;

    public Thread thread, click_checker;

    public boolean loop = true;

    FileUploader fileUploader = new FileUploader();

    NewEncoder() throws IOException, InterruptedException {

        webcam = Webcam.getDefault();
        webcam.setViewSize(size);
        //webcam.open(true);

        /*String input = "";
        Scanner scan = new Scanner(System.in);
        while (!input.equals("stop"))
        {
            input = scan.next();
            if (input.equals("capture"))
            {
                capture();
            }
        }*/
        WebcamPanel panel = new WebcamPanel(webcam, size, false);
        panel.cam_number = cam_number;
        panel.setFillArea(true);

        setTitle("Video Capture");
        setLayout(new FlowLayout());
        panel.start();
        add(panel);

        add(btSnapMe);
        pack();
        setVisible(true);

        thread = new Thread(buffer_collecter);
        thread.start();
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                loop=false;
                e.getWindow().dispose();
                System.exit(0);
            }
        });

        click_checker = new Thread(click_check_runnable);
        click_checker.start();

        btSnapMe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                clicks++;
            }
        });

    }

    public static void main(String[] args) throws IOException, InterruptedException {

        new NewEncoder();

    }

    public void capture() throws IOException, InterruptedException {
        long current = System.currentTimeMillis();

        int i = 0;

        loop = false;
        long start;
        while (current + 5000 > System.currentTimeMillis()) {

            BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
            Graphics2D g2 = image.createGraphics();
            g2.drawString(cam_number, WebcamResolution.QVGA.getSize().width - 50, 20);

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            g2.drawString(dateFormat.format(date).toString(), image.getWidth() - image.getWidth() * 42 / 100, 30);

            IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);

            IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis()) * 1);

            frame.setQuality(0);

            arrayList.add(frame);
        }

        arrayList.get(0).setKeyFrame(true);
        start = arrayList.get(0).getTimeStamp() - 1;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        File file = new File(dateFormat.format(date).toString() + ".mp4");
        writer = ToolFactory.makeWriter(file.getName());
        writer.addVideoStream(0, 0, ICodec.ID.CODEC_ID_MPEG4, size.width, size.height);

        for (IVideoPicture frame : arrayList) {
            long new_time = (frame.getTimeStamp() - start) * 1000;
            frame.setTimeStamp(new_time);

            writer.encodeVideo(0, frame);
            frame.setTimeStamp(new_time / 1000 + start);
        }
        writer.close();
        loop = true;

        logger.info("Video recorded in file: " + file.getAbsolutePath());
        fileUploader.arrayList.add(file.getAbsolutePath());
        fileUploader.run_left++;
        fileUploader.uploadIt();
    }

    public JButton btSnapMe = new JButton("SnapShot");


    Runnable buffer_collecter = (new Runnable() {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException eee) {
                }
                while (loop) {
                    BufferedImage image = ConverterFactory.convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
                    Graphics2D g2 = image.createGraphics();
                    g2.drawString(cam_number, WebcamResolution.QVGA.getSize().width - 50, 20);


                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    g2.drawString(dateFormat.format(date).toString(), image.getWidth() - image.getWidth() * 42 / 100, 30);


                    IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
                    IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis()) * 1);
                    frame.setQuality(0);

                    arrayList.add(frame);

                    while (arrayList.size() > frame_buffer) {
                        arrayList.remove(0);
                    }

                }
            }
        }
    });

    Runnable click_check_runnable = new Runnable() {
        public void run() {
            while (true) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException Ee) {
                }
                if (clicks > 0) {
                    try {
                        capture();
                        clicks--;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }
    };
}