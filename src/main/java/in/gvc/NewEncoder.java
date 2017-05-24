package in.gvc;

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
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver;
import com.xuggle.mediatool.IMediaWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;


public class NewEncoder extends JFrame {
    Webcam webcam;
    IMediaWriter writer;
    public int clicks = 0;
    Dimension size = WebcamResolution.QVGA.getSize();

    Logger logger = LoggerFactory.getLogger(NewEncoder.class);

    ArrayList<BufferedImage> arrayList = new ArrayList<BufferedImage>();

    public String cam_number = "CAM01";
    public int frame_buffer = 100;

    public Thread thread, click_checker;

    public boolean loop = true;

    FileUploader fileUploader = new FileUploader();

    NewEncoder() throws IOException, InterruptedException {

        Webcam.setDriver(new V4l4jDriver()); // this is important.

        //webcam = Webcam.getDefault();
        webcam = Webcam.getWebcams().get(0);
        //webcam.setViewSize(size);
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

        /*SerialPort comPort = SerialPort.getCommPorts()[0];
        comPort.openPort();*/

        WebcamPanel panel = new WebcamPanel(webcam, size, false);
        panel.cam_number = cam_number;
        //panel.setFillArea(true);

        setTitle("Video Capture");
        setLayout(new FlowLayout());
        panel.start();
        add(panel);

        add(btSnapMe);
        pack();
        setVisible(true);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                loop = false;
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

        thread = new Thread(buffer_collecter);
        thread.start();

        // *050,62,62#
        /*InputStream in = comPort.getInputStream();
        try
        {
            while(true)
            {
                while(comPort.bytesAvailable() == 0)
                    Thread.sleep(200);
                String str="";
                int j=0;
                for (j=0;j<11;j++)
                {
                    str += ((char) in.read());
                }

                System.out.println(str);
                StringBuilder builder = new StringBuilder(str);
                if( !(builder.substring(0,1).equals("*") &&
                        builder.substring(builder.length()-1,builder.length()).equals("#")))
                {
                    continue;
                }

                String speed = builder.subSequence(1,builder.indexOf(",")).toString();
                builder.delete(0, builder.indexOf(",")+1);
                String lat = builder.substring(0,builder.indexOf(",")).toString();
                builder.delete(0,builder.indexOf(",")+1);
                String lon = builder.substring(0,builder.length()-1);

                if(Float.parseFloat(speed) > 80)
                {
                    btSnapMe.doClick();
                }
            }

        } catch (Exception e) { e.printStackTrace(); }
        in.close();
        comPort.closePort();*/


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

            BufferedImage image;// = convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);
            image = webcam.getImage();
            Graphics2D g2 = image.createGraphics();
            g2.drawString(cam_number, image.getWidth() - 50, 20);

            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Date date = new Date();
            g2.drawString(dateFormat.format(date).toString(), image.getWidth() - image.getWidth() * 20 / 100, 30);


            arrayList.add(image);
        }

        /*arrayList.get(0).setKeyFrame(true);
        start = arrayList.get(0).getTimeStamp() - 1;*/
        System.out.println("All frames collected");

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        Date date = new Date();
        File file = new File(dateFormat.format(date).toString() + ".mp4");

        CreateVideo.main(arrayList,file);

        loop = true;

        System.out.println("Video recorded in file: " + file.getAbsolutePath());
        /*fileUploader.arrayList.add(file.getAbsolutePath());
        fileUploader.run_left++;
        fileUploader.uploadIt();*/
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
                    //BufferedImage image = convertToType(webcam.getImage(), BufferedImage.TYPE_3BYTE_BGR);

                    BufferedImage image = webcam.getImage();

                    Graphics2D g2 = image.createGraphics();
                    g2.drawString(cam_number, image.getWidth() - 50, 20);


                    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = new Date();
                    g2.drawString(dateFormat.format(date).toString(), image.getWidth() - image.getWidth() * 20 / 100, 30);

                    /*IConverter converter = ConverterFactory.createConverter(image, IPixelFormat.Type.YUV420P);
                    IVideoPicture frame = converter.toPicture(image, (System.currentTimeMillis()) * 1);
                    frame.setQuality(0);*/

                    arrayList.add(image);

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
                    } catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        clicks--;
                    }

                }
            }
        }
    };


    public static BufferedImage convertToType(BufferedImage sourceImage,
                                              int targetType) {

        BufferedImage image;

        // if the source image is already the target type, return the source
        // image
        if (sourceImage.getType() == targetType) {
            image = sourceImage;
        }
        // otherwise create a new image of the target type and draw the new
        // image
        else {
            image = new BufferedImage(sourceImage.getWidth(),
                    sourceImage.getHeight(), targetType);
            image.getGraphics().drawImage(sourceImage, 0, 0, null);
        }

        return image;

    }
}