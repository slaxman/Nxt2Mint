/*
 * Copyright 2016 Ronald W Hoffman.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ScripterRon.Nxt2Mint;

import org.ScripterRon.Nxt2API.Balance;
import org.ScripterRon.Nxt2API.Chain;
import org.ScripterRon.Nxt2API.Crypto;
import org.ScripterRon.Nxt2API.Nxt;
import org.ScripterRon.Nxt2API.NxtException;
import org.ScripterRon.Nxt2API.Response;
import org.ScripterRon.Nxt2API.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.jocl.CL;
import org.jocl.CLException;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.LogManager;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Nxt2Mint will mint a Nxt2 currency
 */
public class Main {

    /** Logger instance */
    public static final Logger log = LoggerFactory.getLogger("org.ScripterRon.Nxt2Mint");

    /** File separator */
    public static String fileSeparator;

    /** Line separator */
    public static String lineSeparator;

    /** User home */
    public static String userHome;

    /** Operating system */
    public static String osName;

    /** Application identifier */
    public static String applicationID;

    /** Application name */
    public static String applicationName;

    /** Application version */
    public static String applicationVersion;

    /** Application properties */
    public static Properties properties;

    /** Data directory */
    public static String dataPath;

    /** Application properties file */
    private static File propFile;

    /** Enable the GUI */
    public static boolean enableGUI = true;

    /** Main application window */
    public static MainWindow mainWindow;

    /** Application lock file */
    private static RandomAccessFile lockFile;

    /** Application lock */
    private static FileLock fileLock;

    /** Deferred exception text */
    private static String deferredText;

    /** Deferred exception */
    private static Throwable deferredException;

    /** Nxt node host name */
    public static String nxtHost = "localhost";

    /** Nxt API port */
    public static int apiPort = 27876;

    /** Use HTTPS connections */
    public static boolean useSSL = false;

    /** Secret phrase */
    public static String secretPhrase;

    /** Currency code */
    public static String currencyCode;

    /** Currency identifier */
    public static long currencyId;

    /** Currency decimals */
    public static int currencyDecimals;

    /** Currency units */
    public static double currencyUnits;

    /** CPU worker thread count */
    public static int cpuThreads = 1;

    /** GPU intensity */
    public static int gpuIntensity = 0;

    /** GPU devices */
    public static final List<Integer> gpuDevices = new ArrayList<>();

    /** GPU work group sizes */
    public static final List<Integer> gpuSizes = new ArrayList<>();

    /** GPU work group counts */
    public static final List<Integer> gpuCounts = new ArrayList<>();

    /** Minting account identifier */
    public static long accountId;

    /** Minting account public key */
    public static byte[] publicKey;

    /** Child chain */
    public static Chain childChain;

    /** Child chain name (default to IGNIS) */
    public static String chainName = "IGNIS";

    /** Transaction fee (default to 1.0) */
    public static long fee = 100000000L;

    /** Minting units expressed as a whole number with an implied decimal point */
    public static long mintingUnits;

    /** Minting algorithm */
    public static int mintingAlgorithm;

    /** Minting target */
    public static MintingTarget mintingTarget;

    /** GPU devices */
    public static final List<GpuDevice> gpuDeviceList = new ArrayList<>();

    /**
     * Handles program initialization
     *
     * @param   args                Command-line arguments
     */
    public static void main(String[] args) {
        try {
            fileSeparator = System.getProperty("file.separator");
            lineSeparator = System.getProperty("line.separator");
            userHome = System.getProperty("user.home");
            osName = System.getProperty("os.name").toLowerCase();
            //
            // Process command-line options
            //
            dataPath = System.getProperty("nxt.datadir");
            if (dataPath == null) {
                if (osName.startsWith("win"))
                    dataPath = userHome+"\\Appdata\\Roaming\\Nxt2Mint";
                else if (osName.startsWith("linux"))
                    dataPath = userHome+"/.Nxt2Mint";
                else if (osName.startsWith("mac os"))
                    dataPath = userHome+"/Library/Application Support/Nxt2Mint";
                else
                    dataPath = userHome+"/Nxt2Mint";
            }
            //
            // Create the data directory if it doesn't exist
            //
            File dirFile = new File(dataPath);
            if (!dirFile.exists())
                dirFile.mkdirs();
            //
            // Initialize the logging properties from 'logging.properties'
            //
            File logFile = new File(dataPath+fileSeparator+"logging.properties");
            if (logFile.exists()) {
                FileInputStream inStream = new FileInputStream(logFile);
                LogManager.getLogManager().readConfiguration(inStream);
            }
            //
            // Use the brief logging format
            //
            BriefLogFormatter.init();
            //
            // Process configuration file options
            //
            processConfig();
            if (secretPhrase==null || secretPhrase.length() == 0)
                throw new IllegalArgumentException("Secret phrase not specified");
            if (currencyCode==null || currencyCode.length()<3 || currencyCode.length()>5)
                throw new IllegalArgumentException("Currency code is not valid");
            if (gpuIntensity > 1048576)
                throw new IllegalArgumentException("Maximum gpuIntensity is 1,048,576");
            publicKey = Crypto.getPublicKey(secretPhrase);
            accountId = Utils.getAccountId(publicKey);
            //
            // Get the application build properties
            //
            Class<?> mainClass = Class.forName("org.ScripterRon.Nxt2Mint.Main");
            try (InputStream classStream = mainClass.getClassLoader().getResourceAsStream("META-INF/application.properties")) {
                if (classStream == null)
                    throw new IllegalStateException("Application build properties not found");
                Properties applicationProperties = new Properties();
                applicationProperties.load(classStream);
                applicationID = applicationProperties.getProperty("application.id");
                applicationName = applicationProperties.getProperty("application.name");
                applicationVersion = applicationProperties.getProperty("application.version");
            }
            log.info(String.format("%s Version %s", applicationName, applicationVersion));
            log.info(String.format("Application data path: %s", dataPath));
            log.info(String.format("Using Nxt node at %s://%s:%d", (useSSL ? "https" : "http"), nxtHost, apiPort));
            log.info(String.format("Minting %,f units of %s for account %s: %d CPU threads, %d GPU intensity",
                                   currencyUnits, currencyCode, Utils.getAccountRsId(accountId),
                                   cpuThreads, gpuIntensity));
            //
            // Open the application lock file
            //
            lockFile = new RandomAccessFile(dataPath+fileSeparator+".lock", "rw");
            fileLock = lockFile.getChannel().tryLock();
            if (fileLock == null) {
                JOptionPane.showMessageDialog(null, "Nxt2Mint is already running", "Error",
                                              JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
            //
            // Load the saved application properties
            //
            propFile = new File(dataPath+fileSeparator+"Nxt2Mint.properties");
            properties = new Properties();
            if (propFile.exists()) {
                try (FileInputStream in = new FileInputStream(propFile)) {
                    properties.load(in);
                }
            }
            //
            // Start the minter
            //
            startup();
        } catch (Exception exc) {
            if ((exc instanceof IllegalArgumentException))
                log.error(exc.getMessage());
            else
                log.error("Exception during program initialization", exc);
        }
    }

    /**
     * Start the minter
     */
    @SuppressWarnings("unchecked")
    private static void startup() {
        try {
            //
            // Initialize the Nxt API library
            //
            Nxt.init(nxtHost, apiPort, useSSL);
            childChain = Nxt.getChain(chainName);
            if (childChain == null)
                throw new IllegalArgumentException(String.format("Chain '%s' is not defined", chainName));
            //
            // Ensure the account is funded
            //
            if (childChain.getDecimals() != 8) {
                fee = BigDecimal.valueOf(fee, 8).movePointRight(childChain.getDecimals()).longValue();
            }
            long balance = Nxt.getBalance(accountId, childChain).getBalance();
            if (balance < fee)
                throw new IllegalArgumentException(
                        String.format("Account %s confirmed balance is less than the transaction fee",
                                Utils.getAccountRsId(accountId)));
            //
            // Get the currency definition
            //
            Response response = Nxt.getCurrency(currencyCode, childChain);
            currencyId = response.getId("currency");
            currencyDecimals = response.getInt("decimals");
            long maxSupply = response.getLong("maxSupplyQNT");
            long reserveSupply = response.getLong("reserveSupplyQNT");
            List<String> types = response.getStringList("types");
            if (!types.contains("MINTABLE"))
                throw new IllegalArgumentException(
                        String.format("Currency %s is not mintable", currencyCode));
            mintingAlgorithm = response.getInt("algorithm");
            if (!HashFunction.isSupported(mintingAlgorithm))
                throw new IllegalArgumentException(
                        String.format("Currency algorithm %d is not supported", mintingAlgorithm));
            if (gpuIntensity > 0 && !GpuFunction.isSupported(mintingAlgorithm))
                throw new IllegalArgumentException(
                        String.format("Currency algorithm %d is not supported on the GPU", mintingAlgorithm));
            //
            // Get the current minting target
            //
            mintingUnits = BigDecimal.valueOf(currencyUnits).movePointRight(currencyDecimals).longValue();
            response = Nxt.getMintingTarget(currencyId, accountId, mintingUnits);
            mintingTarget = new MintingTarget(response);
            if (mintingUnits > maxSupply - reserveSupply)
                throw new IllegalArgumentException(
                        String.format("Maximum minting units is %s for currency %s",
                                BigDecimal.valueOf(maxSupply - reserveSupply, currencyDecimals).toPlainString(),
                                currencyCode));
            //
            // Get the GPU device list if GPU intensity is non-zero
            //
            if (gpuIntensity > 0) {
                if (gpuDevices.isEmpty()) {
                    gpuDevices.add(0);
                    gpuSizes.add(256);
                    gpuCounts.add(0);
                }
                buildGpuList();
                for (int i=0; i<gpuDevices.size(); i++) {
                    int devnum = gpuDevices.get(i);
                    if (devnum >= gpuDeviceList.size())
                        throw new IllegalArgumentException(
                                String.format("GPU device %d is not available", devnum));
                    GpuDevice gpuDevice = gpuDeviceList.get(devnum);
                    if (gpuSizes.get(i) > gpuDevice.getMaxWorkGroupSize()) {
                        log.warn(String.format(
                                "Work group size %d for GPU %d exceeds maximum size %d - using maximum size",
                                gpuSizes.get(i), devnum, gpuDevice.getMaxWorkGroupSize()));
                        gpuDevice.setWorkGroupSize(gpuDevice.getMaxWorkGroupSize());
                    } else {
                        gpuDevice.setWorkGroupSize(gpuSizes.get(i));
                    }
                    gpuDevice.setWorkGroupCount(gpuCounts.get(i));
                }
            }
            //
            // Start the GUI
            //
            if (enableGUI) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                javax.swing.SwingUtilities.invokeLater(() -> createAndShowGUI());
                while (mainWindow == null)
                    Thread.sleep(1000);
            }
            //
            // Start minting
            //
            Mint.mint();
        } catch (IllegalArgumentException exc) {
            log.error(exc.getMessage());
            shutdown();
        } catch (IOException exc) {
            log.error("Unable to obtain initial minting information", exc);
            shutdown();
        } catch (Exception exc) {
            log.error("Exception while obtaining initial minting information", exc);
            shutdown();
        }
    }

    /**
     * Create and show our application GUI
     *
     * This method is invoked on the AWT event thread to avoid timing
     * problems with other window events
     */
    private static void createAndShowGUI() {
        //
        // Use the normal window decorations as defined by the look-and-feel
        // schema
        //
        JFrame.setDefaultLookAndFeelDecorated(true);
        //
        // Create the main application window
        //
        mainWindow = new MainWindow();
        //
        // Show the application window
        //
        mainWindow.pack();
        mainWindow.setVisible(true);
    }

    /**
     * Shutdown and exit
     */
    public static void shutdown() {
        //
        // Stop minting
        //
        Mint.shutdown();
        //
        // Save the application properties
        //
        saveProperties();
        //
        // Close the application lock file
        //
        try {
            fileLock.release();
            lockFile.close();
        } catch (IOException exc) {
        }
        //
        // All done
        //
        System.exit(0);
    }

    /**
     * Save the application properties
     */
    public static void saveProperties() {
        try {
            try (FileOutputStream out = new FileOutputStream(propFile)) {
                properties.store(out, "Nxt2Mint Properties");
            }
        } catch (IOException exc) {
            log.error("Exception while saving application properties", exc);
        }
    }

    /**
     * Process the configuration file
     *
     * @throws      IllegalArgumentException    Invalid configuration option
     * @throws      IOException                 Unable to read configuration file
     */
    private static void processConfig() throws IOException, IllegalArgumentException {
        //
        // Use the defaults if there is no configuration file
        //
        File configFile = new File(dataPath+Main.fileSeparator+"Nxt2Mint.conf");
        if (!configFile.exists())
            return;
        //
        // Process the configuration file
        //
        try (BufferedReader in = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line=in.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.charAt(0) == '#')
                    continue;
                int sep = line.indexOf('=');
                if (sep < 1)
                    throw new IllegalArgumentException(String.format("Invalid configuration option: %s", line));
                String option = line.substring(0, sep).trim().toLowerCase();
                String value = line.substring(sep+1).trim();
                try {
                    switch (option) {
                        case "connect":
                            nxtHost = value;
                            break;
                        case "apiport":
                            apiPort = Integer.valueOf(value);
                            break;
                        case "usessl":
                            useSSL = Boolean.valueOf(value);
                            break;
                        case "secretphrase":
                            secretPhrase = value;
                            break;
                        case "chain":
                            chainName = value.toUpperCase();
                            break;
                        case "fee":
                            fee = new BigDecimal(value).movePointRight(8).longValue();
                            break;
                        case "currency":
                            currencyCode = value;
                            break;
                        case "units":
                            currencyUnits = Double.valueOf(value);
                            break;
                        case "cputhreads":
                            cpuThreads = Integer.valueOf(value);
                            break;
                        case "gpuintensity":
                            gpuIntensity = Integer.valueOf(value);
                            break;
                        case "gpudevice":
                            String[] splits = value.split(",");
                            gpuDevices.add(Integer.valueOf(splits[0].trim()));
                            if (splits.length > 1) {
                                gpuSizes.add(Integer.valueOf(splits[1].trim()));
                                if (splits.length > 2) {
                                    gpuCounts.add(Integer.valueOf(splits[2].trim()));
                                } else {
                                    gpuCounts.add(0);
                                }
                            } else {
                                gpuSizes.add(256);
                                gpuCounts.add(0);
                            }
                            break;
                        case "enablegui":
                            if (value.equalsIgnoreCase("true"))
                                enableGUI = true;
                            else if (value.equalsIgnoreCase("false"))
                                enableGUI = false;
                            else
                                throw new IllegalArgumentException(String.format("enableGUI must be TRUE or FALSE"));
                            break;
                        default:
                            throw new IllegalArgumentException(String.format("Invalid configuration option: %s", line));
                    }
                } catch (NumberFormatException exc) {
                    throw new IllegalArgumentException(String.format("Invalid numeric value for '%s' option",
                                                       option));
                }
            }
        }
    }

    /**
     * Build a list of available GPU devices
     *
     * @throws      CLException         OpenCL error occurred
     */
    private static void buildGpuList() throws CLException {
        //
        // Enable OpenCL exceptions
        //
        CL.setExceptionsEnabled(true);
        //
        // Get the available platforms
        //
        int[] numPlatforms= new int[1];
        CL.clGetPlatformIDs(0, null, numPlatforms);
        cl_platform_id[] platforms = new cl_platform_id[numPlatforms[0]];
        CL.clGetPlatformIDs(platforms.length, platforms, null);
        //
        // Get the devices for each platform
        //
        for (cl_platform_id platform : platforms) {
            int numDevices[] = new int[1];
            CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, 0, null, numDevices);
            cl_device_id[] devices = new cl_device_id[numDevices[0]];
            CL.clGetDeviceIDs(platform, CL.CL_DEVICE_TYPE_ALL, devices.length, devices, null);
            for (cl_device_id device : devices) {
                long deviceType = OpenCL.getLong(device, CL.CL_DEVICE_TYPE);
                if ((deviceType&CL.CL_DEVICE_TYPE_GPU)!=0 && OpenCL.getBoolean(device, CL.CL_DEVICE_AVAILABLE)) {
                    String platformName = OpenCL.getString(platform, CL.CL_PLATFORM_NAME);
                    String deviceName = OpenCL.getString(device, CL.CL_DEVICE_NAME);
                    String driverVersion = OpenCL.getString(device, CL.CL_DRIVER_VERSION);
                    int computeUnits = OpenCL.getInt(device, CL.CL_DEVICE_MAX_COMPUTE_UNITS);
                    long globalMemorySize = OpenCL.getLong(device, CL.CL_DEVICE_GLOBAL_MEM_SIZE);
                    long localMemorySize = OpenCL.getLong(device, CL.CL_DEVICE_LOCAL_MEM_SIZE);
                    int maxWorkGroupSize = (int)OpenCL.getSize(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE);
                    int gpuId = gpuDeviceList.size();
                    log.info(String.format(
                            "GPU device %d: %s, %s, Driver %s\n" +
                            "  %dMB global memory, %dKB local memory, %d compute units, Max work group size %d",
                            gpuId, platformName, deviceName, driverVersion, globalMemorySize/(1024*1024),
                            localMemorySize/1024, computeUnits, maxWorkGroupSize));
                    gpuDeviceList.add(new GpuDevice(gpuId, platform, device, computeUnits,
                                                    globalMemorySize, localMemorySize, maxWorkGroupSize));
                }
            }
        }
    }

    /**
     * Display a dialog when an exception occurs.
     *
     * @param       text        Text message describing the cause of the exception
     * @param       exc         The Java exception object
     */
    public static void logException(String text, Throwable exc) {
        if (SwingUtilities.isEventDispatchThread()) {
            StringBuilder string = new StringBuilder(512);
            //
            // Display our error message
            //
            string.append("<html><b>");
            string.append(text);
            string.append("</b><br>");
            //
            // Display the exception object
            //
            string.append("<b>");
            if (exc instanceof NxtException)
                string.append(exc.getMessage());
            else
                string.append(exc.toString());
            string.append("</b><br><br>");
            //
            // Display the stack trace
            //
            if (!(exc instanceof NxtException)) {
                StackTraceElement[] trace = exc.getStackTrace();
                int count = 0;
                for (StackTraceElement elem : trace) {
                    string.append("<br>");
                    string.append(elem.toString());
                    if (++count == 25)
                        break;
                }
            }
            string.append("</html>");
            JOptionPane.showMessageDialog(mainWindow, string, "Error", JOptionPane.ERROR_MESSAGE);
        } else if (deferredException == null) {
            deferredText = text;
            deferredException = exc;
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    Main.logException(deferredText, deferredException);
                    deferredException = null;
                    deferredText = null;
                });
            } catch (Exception logexc) {
                log.error("Unable to log exception during program initialization");
            }
        }
    }
}
