Nxt2Mint
=======

Nxt2Mint mints currencies defined by the Nxt2 Monetary System.  A single currency can be minted as specified by the NxtMint configuration file.  The minting algorithm is executed using one or more CPU threads or GPU work items.  Newly-minted coins will be added to the account specified in the configuration file.     

The NRS node used to create the mint transactions must accept API connections.  This is done by specifying nxt.apiServerPort, nxt.apiServerHost and nxt.allowedBotHosts in nxt.properties.  The account secret phrase is not sent to the Nxt server since the mint transactions are signed locally.  

Nxt2Mint requires the Java 8 runtime since it uses language features that are not available in earlier versions of Java.   

OpenCL is used to mint using the GPU and is not needed if you are using just the CPU.  You will need to obtain OpenCL from your graphics card vendor (OpenCL may be automatically installed as part of the graphics card driver installation).


Installation
============

Application data will be stored in a system-specific directory unless you specify your own directory using the -Dnxt.datadir command-line option.  The default directories are:

  - Linux: user-home/.Nxt2Mint	    
  - Mac: user-home/Library/Application Support/Nxt2Mint    
  - Windows: user-home\AppData\Roaming\Nxt2Mint	    
  
Perform the following steps to install Nxt2Mint on your system:

  - Install the Java 8 runtime if you do not already have it installed.     
  - Download the latest version from https://github.com/ScripterRon/Nxt2Mint/releases.       
  - Extract the files from the archive in a directory of your choice.   
  - Copy Nxt2Mint.conf to the application data directory.  Edit the file to specify your desired NRS server, the child chain for the currency, your secret passphrase (the passphrase will not be sent to the server) and the desired number of CPU threads and/or GPU intensity.  If you are using the GPU, start with gpuIntensity=1 and gpuDevice=0,32,32 and then increase the values until either there is no further improvement in the hash rate or your graphics card begins to overheat.  Your device driver will fail to load the OpenCL kernel if you exceed the available resources (this is especially true for Scrypt since it has a large memory requirement).  The global size (work group size * work group count) determines how much storage is required.    
  - Copy logging.properties to the application data directory.  Edit the log file name if you want to place it somewhere other than the temporary directory for your userid.     
  - Install OpenCL if you want to use the GPU for mining.  The OpenCL runtime library must be in PATH (Windows) or LD_LIBRARY_PATH (Linux).
  - Edit the mint.sh (Linux/Macintosh) or mint.bat (Windows) to fit your needs. 


Build
=====

You can build Nxt2Mint from the source code if you do not want to use the packaged release files.  I use the Netbeans IDE but any build environment with Maven and the Java compiler available should work.  The documentation is generated from the source code using javadoc.

Here are the steps for a manual build.  You will need to install Maven 3 and Java SE Development Kit 8 if you don't already have them.

  - Create the executable: mvn clean package    
  - [Optional] Copy target/Nxt2Mint-v.r.m.jar and lib/* to wherever you want to store the executables.    


Runtime Options
===============

The following command-line options can be specified using -Dname=value

  - nxt.datadir=directory-path		
    Specifies the application data directory. Application data will be stored in a system-specific directory if this option is omitted:		
	    - Linux: user-home/.Nxt2Mint	    
		- Mac: user-home/Library/Application Support/Nxt2Mint    
		- Windows: user-home\AppData\Roaming\Nxt2Mint	    
	
  - java.util.logging.config.file=file-path		
    Specifies the logger configuration file. The logger properties will be read from 'logging.properties' in the application data directory. If this file is not found, the 'java.util.logging.config.file' system property will be used to locate the logger configuration file. If this property is not defined, the logger properties will be obtained from jre/lib/logging.properties.
	
    JDK FINE corresponds to the SLF4J DEBUG level	
	JDK INFO corresponds to the SLF4J INFO level	
	JDK WARNING corresponds to the SLF4J WARN level		
	JDK SEVERE corresponds to the SLF4J ERROR level		

The following configuration options can be specified in NxtMint.conf.  This file is required and must be in the application directory.	

  - connect=host    
    Specifies the NRS host name and defaults to 'localhost'		
	
  - apiPort=port		
	Specifies the NRS API port and defaults to 27876.  Use 26876 for testnet.    
    
  - useSSL=boolean      
    Specify 'true' to use HTTPS or 'false' to use HTTP to connect to the NRS node.  HTTP is always used for a localhost connection.  The default is 'false'.
    
  - secretPhrase=phrase     
    Specifies the account secret phrase and must be specified.  The secret phrase will not be sent to the NRS server.   
    
  - chain=name    
    Specifies the Nxt2 child chain for the currency and defaults to IGNIS.  The currency must have been issued for this chain and the minting transaction fees will be paid with this chain.   

  - fee=amount    
    Specifies the fee you want to pay for each minting transaction.  Each minting transaction costs 1 ARDR, so the fee must be large enough to result in at least 1 ARDR using the best available bundler exchange rate.  The default fee is 1.00 if no fee is specified.  
    
  - currency=code      
    Specifies the code for the currency to be minted.  The currency must be defined for the specified chain.       

  - units=count     
    Specifies the number of units to generate for each hash round and defaults to 1.  The hash difficulty increases as the number of units increases but the transaction fee is the same no matter how many units are generated.  Thus you want to increase units as much as possible to reduce the cost of minting the currency but don't set it so high that you don't mint anything during a session.  The count can be specified as an integer value or as a decimal value with a maximum number of digits following the decimal point as defined for the currency.        
    
  - cpuThreads=count       
    Specifies the number of CPU threads to be used and defaults to 1.  Specifying a thread count greater than the number of CPU processors will not improve minting since the mint algorithms are CPU-intensive and will drive each processor to 100% utilization.  Decrease the thread count if your computer becomes too hot or system response degrades significantly.  No CPU threads will be used if cpuThreads is 0.     
    
  - gpuIntensity=count    
    Specifies the total number of GPU work items multiplied by 1024.  A GPU will not be used if gpuIntensity is 0.  gpuIntensity is an integer between 0 and 1,048,576 and defaults to 0.  Your graphics card must support OpenCL in order to use the GPU.  You will need to try different values to determine an acceptable hash rate.  Specifying too large a value can result in performance degradation and GPU memory errors.  Start with an initial value of 10 and raise or lower needed.  Set gpuDevice=0,n,0 where n is the number of cores per compute unit for your adapter.      
    
  - gpuDevice=index,wsize,gcount	
    Specifies the GPU device number (0, 1, 2, ...), the work group size and the work group count.  The first GPU device will be used if this parameter is omitted.  This parameter can be repeated to use multiple GPU devices.  The GPU devices that are available are listed when NxtMint starts if a non-zero value for gpuIntensity is specified.  

    The work group size specifies the number of work items per work group and defaults to 256.  Performance can sometimes be improved by setting the work group size to the number of cores in a compute unit.  You can determine this value by dividing the number of cores on the card by the number of compute units.  In addition, each card has a preferred work item multiple.  For example, if the preferred multiple is 32, work item sizes that are a multiple of 32 will often give better performance (unless there are resource limitations or memory contention).    
    
    The work group count specifies the number of work groups per kernel execution.  If this parameter is zero, the number of work groups is determined by the gpuIntensity value.  The number of work items per kernel execution is (work group size * work group count).  Multiple kernel execution passes will be performed if the work group count is smaller than the number required by the gpuIntensity.  For example, gpuIntensity=10 means there will be a total of 10,240 work items.  If gpuDevice=0,32,64, then there will be 2048 work items per kernel execution.  This means there will be 5 execution passes before control returns to the Java mint worker.  If gpuDevice=0,32,0, then the group count will be calculated as (total work items / work group size) = 10,240/32 or 320.  This means there will be 1 execution pass before control returns to the Java mint worker.  A single execution pass gives the best hash rate but your adapter card may place an upper limit on the global size (number of work items in a single kernel execution).  NxtMint will display the calculated values for local size (work items per work group), global size (total work items per kernel execution) and number of kernel passes when it starts a GPU worker.
    
  - enableGUI=true|false      
    Specifies whether or not to enable the GUI and defaults to true.  Disabling the GUI allows NxtMint to run in headless environments such as a disconnected service.      
	
