package com.revivermx.platform.datatypes.protobuf;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortPacketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TestPlateSerial implements SerialPortPacketListener, Runnable {
    private static final Logger logger = LoggerFactory.getLogger(TestPlateSerial.class);
    public static final String TERMINATE_COMMAND = "\r\n";
    public static final int READ_BYTE_LEN = 10;
    private final String portDesc;
    OutputStream out;
    InputStream in;
    SerialPort comPort;
    int bytesPerRead;
    public TestPlateSerial(int bytesPerRead,String portDescription) {
        this.bytesPerRead = bytesPerRead;
        this.portDesc = portDescription;
    }

    public void start() throws IOException {
        comPort = findUartPort(portDesc);
        if(comPort == null)
            return;
        comPort.setBaudRate(115200);
        comPort.openPort();


        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 50, 0);
        logger.debug("Opened port "+comPort.getSystemPortName()+" at baudRate of "+comPort.getBaudRate());
        out = comPort.getOutputStream();
        String write = TERMINATE_COMMAND;
        out.write(write.getBytes(),0,write.getBytes().length);

        //comPort.addDataListener(this);
        //try { Thread.sleep(5000); } catch (Exception e) { e.printStackTrace(); }

//        in = comPort.getInputStream();
//        try
//        {
//            for (int j = 0; j < 1000; ++j)
//                System.out.print((char)in.read());
//            in.close();
//        } catch (Exception e) { e.printStackTrace(); }
        //comPort.closePort();
    }

    public static void main(String[] args) {
        String portDesc = "CP2104 USB to UART Bridge Controller";
        TestPlateSerial tester = new TestPlateSerial(100,portDesc);
        ExecutorService executor = Executors.newSingleThreadExecutor();
            //executor.submit(tester);
            //tester.run();
            Scanner scanner = new Scanner(System.in);
            tester.run();

            while(true) {
                logger.error("Enter your command: ");
                String command = scanner.nextLine();
                if(command.equalsIgnoreCase("quit") || command.equalsIgnoreCase("exit")) {
                    tester.close();
                    System.exit(0);
                } else {
                    String[] commands =  {command};
                    FWSerialCommand cmd = tester.createCommand(commands,7,"ENV");
                    executor.submit(cmd);
                    String resp = cmd.getCommandResponse();
                    logger.info("Response from command is "+resp);
                    //tester.sendCommand(command);
                }
            }
            //tester.close();
    }

    private void sendCommand(String command) {
        command = command+TERMINATE_COMMAND;
        //logger.debug("Sending command to UART. "+command);
        try {
            out.write(command.getBytes(),0,command.getBytes().length);
            out.flush();
        } catch (IOException e) {
            logger.error("Error occurred while writing to UART. "+e.getMessage(),e);
        }
    }

    private void close() {
        comPort.closePort();
    }

    private SerialPort findUartPort(String descriptivePortName) {
        SerialPort[] comPorts = SerialPort.getCommPorts();
        SerialPort comPort = null;
        for (SerialPort port:comPorts) {
            if(port != null && port.getDescriptivePortName().equalsIgnoreCase("CP2104 USB to UART Bridge Controller")) {
                comPort = port;
            }
        }
        return comPort;
    }

    @Override
    public int getPacketSize() {
        return bytesPerRead;
    }

    @Override
    public int getListeningEvents() {
        return SerialPort.LISTENING_EVENT_DATA_RECEIVED;
    }

    @Override
    public void serialEvent(SerialPortEvent event) {
        byte[] newData = event.getReceivedData();
        //logger.debug("Received data of size: " + newData.length);
        StringBuffer t = new StringBuffer();
        for (int i = 0; i < newData.length; ++i)
            t.append((char)newData[i]);
        logger.debug(t.toString());
        //logger.debug("Done receiving this batch of data.");
    }

    @Override
    public void run() {
        try {
            this.start();
        } catch (IOException e) {
            logger.error("Error occurred while starting up the comport."+e.getMessage(),e);
        }
    }

    public FWSerialCommand createCommand(String[] commands,int timeoutSecs, String strMatch) {
        return new FWSerialCommand(commands,timeoutSecs,strMatch);
    }

    class FWSerialCommand implements Runnable {
        private final String[] commands;
        private int timeout;
        private String successStr;
        StringBuffer commandResponse;
        public Logger logger = LoggerFactory.getLogger(FWSerialCommand.class);
        private long endTime;
        private final Object lock = new Object();
        private volatile Integer byteCount = null;
        private boolean isValid = false;

        public FWSerialCommand(String[] commands, int timeoutSecs, String successStrMatch) {
            this.commands = commands;
            this.timeout = timeoutSecs;
            this.successStr = successStrMatch;
            commandResponse = new StringBuffer();
            //comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, timeout*1000, 0);
        }

        public void runCommand() {
            for (String command: commands) {
                command = command+TERMINATE_COMMAND;
                logger.info(command);
                try {
                    out.write(command.getBytes(),0,command.getBytes().length);
                } catch (IOException e) {
                    logger.error(e.getMessage(),e);
                }
            }
            try {
                out.flush();
                endTime = System.currentTimeMillis()+(timeout*1000);
            } catch (IOException e) {
                logger.error(e.getMessage(),e);
            }
        }

        private void collectPlateResponse() {
            while(System.currentTimeMillis()<= endTime) {
                byte[] readBuffer = new byte[READ_BYTE_LEN];
                int numRead = comPort.readBytes(readBuffer, readBuffer.length);

                for (int i = 0; i < readBuffer.length; ++i)
                    commandResponse.append((char)readBuffer[i]);

                //commandResponse.append(readBuffer.toString());
            }
        }
        @Override
        public void run() {
            runCommand();
            collectPlateResponse();
            validateCommandResponse();
            byteCount = commandResponse.length();
            synchronized (lock) {
                lock.notify();
            }
        }

        public String getCommandResponse() {
            synchronized (lock) {
                while (byteCount == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                }
                return commandResponse.toString();
            }
        }

        private void validateCommandResponse() {
            if(commandResponse.toString().contains(successStr)) {
                isValid = true;
            } else
                isValid = false;

        }
    }
}
