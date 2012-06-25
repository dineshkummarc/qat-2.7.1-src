/* 
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

#include "Utils.hpp"


#ifndef MSG_WAITALL
#define MSG_WAITALL 0
#endif

/**
 * Create the file specified in fileName. If the file already exists
 * no action is taken.
 */
void Utils::touch(string fileName) 
{
  ofstream ofsOut(fileName.c_str(),ios::app);
  ofsOut.close();  
}

int Utils::createH(string fileName) 
{
	int handle = open(fileName.c_str(), O_CREAT | O_RDWR, 0666);
	chmod(fileName.c_str(),0666);
	
	if (handle<0)
		printf("Error:Could not create the file\n");
	return handle;
}

FILE* Utils::createF(string fileName) 
{
	FILE *file_ptr = NULL;
	if ((file_ptr = fopen(fileName.c_str(),"w"))==NULL) {
		printf("Error: Could not create the file %s",fileName.c_str());
	}
	else {	  
	  chmod(fileName.c_str(),0666);
	}	
	return file_ptr;	
}

int Utils::openH(string fileName) 
{
	int handle = open(fileName.c_str(), O_RDWR);
	if (handle<0)
		printf("Error:Could not open the file %s\n",fileName.c_str());
	return handle;
}

FILE* Utils::openF(string fileName) 
{
	FILE *file_ptr=NULL;
	if ((file_ptr = fopen(fileName.c_str(),"r+"))==NULL) {
		printf("Error:Could not open the file %s",fileName.c_str());
	}
	return file_ptr;	
}

int Utils::delete_file(string fileName) 
{
	return remove(fileName.c_str());
}

/**
 * This method returns TRUE if the directory or file path specified
 * by fileName exists, else it
 * returns false.
*/
int Utils::exists(string fileName) 
{
  ifstream ofsOut(fileName.c_str(),ios::nocreate);
  
  if ((ofsOut.good())&&
	  (!ofsOut.fail())) {
	ofsOut.close();
	return TRUE;
  }
  else
	ofsOut.close();
	return FALSE;  
}

/**
 * Returns the size of the file specified in fileName, or
 * -1 if an error occurs.
 */
long Utils::size(string fileName) 
{
  // TODO - this does not work under Workshop CC if
  // the file is already open - always returns zero.
  // This happends when we duplicate the stdout/stderr of a child
  // since we can never really close the child output
  // handles.
  // Compiled with Gnu it works fine.
  struct stat st;
  //check if it exists first
  if (stat(fileName.c_str(),
		   &st)==0) {	  
	
	return st.st_size;
  }
  return -1;	
}

	
string Utils::substring(string src, int start, int end) 
{
  string dest; 
  for (int i =0; i < (end-start); i++)
	dest += src[i+start];
  return dest;	
}

int Utils::lastIndexOf(string str, char c) 
{
	for (int i =str.length()-1; i > 0; i--)
		if (str[i]==c)
			return i;
	return -1;	
}

/**
 * This method will create the directory dirName as well as any 
 * preceding path components
 * if they don't already exist.
 * Returns zero upon successful completion.
 */
int Utils::mk_dirp(const string dirName) 
{
  const char fileSeparator= '/';	
  string node;
  int result;
  int pos = lastIndexOf(dirName, fileSeparator);	
  if (pos>0) {
	node = substring(dirName, 0, pos);
	result = mk_dirp(node);
  }
  if (!exists(dirName)) {			
	return mk_dir(dirName);
  }
  else {
	return result;	
  }			
}

/**
 * This method will create the directory dirName as well as any 
 * preceding path components
 * if they don't already exist.
 */
int Utils::mk_dir(string dirName) 
{  
  return mkdir(dirName.c_str(),0755);
}

int Utils::readByte(unsigned char *buff, int count, FILE *file) 
{
	int bytesRead = fread(buff,sizeof(unsigned char),count,file);
// 	if (bytesRead != count) {
// 		printf("readByte ERROR!!!!! EOF reached! %d\n",count);		
// 	}
	return bytesRead;	
}

int Utils::readChar(char *buff, int count, FILE *file) 
{
	int bytesRead = fread(buff,sizeof(char),count,file);
// 	if (bytesRead != count) {
// 		printf("readChar ERROR!!!!! EOF reached! %d\n",count);		
// 	}
	return bytesRead;	
}

int Utils::cleanUnzippedFiles(string zipFile, string workDir)
{	
	char *dirList[STR_ARR_LENGTH];
	int dirListCount = 0;
	FILE *filePtr;
	int result = 0;
	int fileNameLength;
	char buff[STR_LENGTH];
	char *fileName;
	  
	// open the zip file to read central dir structure
	if ((filePtr = fopen(zipFile.c_str(),"r"))!=NULL) {
		int bytesRead;			
		unsigned char frame[4];
		unsigned char frame_buffer[24];
			
		memset(frame,0,4);
					  
		do {
			// shift the frame to the next byte
			for (int i = 0; i < 3; i++)
				frame[i] = frame[i+1];	
			bytesRead = readByte(&frame[3],1,filePtr);
			if (getLong(frame)==0x04034b50) {
				bytesRead = readByte(frame_buffer,22,filePtr);
				bytesRead = readByte(frame_buffer,2,filePtr); // 2 LOCLEN
				fileNameLength = getInt(frame_buffer);
				bytesRead = readByte(frame_buffer,2,filePtr); // 2 LOCFILE
					
				// now read file name
				fileName = Utils::allocateChars(STR_LENGTH);
				bytesRead = readChar(buff,fileNameLength,filePtr);
				buff[fileNameLength]='\0';					
				strcpy(fileName,workDir.c_str());
				fileName[workDir.length()]='\0';
				strcat(fileName,"/");
				strcat(fileName,buff);
				ConsoleServer::debugMsg(1,"Deleting :%s\n",fileName);
				if (delete_file(fileName)!=0) {
					// might be a directory, so try delete it last
					dirList[dirListCount++] = fileName;
				}
				else {					
					delete [] fileName;
					fileName = NULL;					  
				}
			}
		} while(bytesRead>0);
		fclose(filePtr);
			
		// clear directory entries now
		for (int i = (dirListCount-1); i >=0; i--) {
			rmdir(dirList[i]);
			delete [] dirList[i];
			dirList[i] = NULL;				
		}			
	}		
	else {
		ConsoleServer::debugMsg(1,"Error opening zip file %s\n",zipFile.c_str());
		result = -1;
	}	
	return result;
}

int Utils::getInt(const unsigned char c[])
{
	return (int)(c[1] << 8 | c[0]);
}

long Utils::getLong(const unsigned char c[])
{
	return (long)(c[3] << 24 |
				  c[2] << 16 |
				  c[1] << 8 |
				  c[0]);
}


int Utils::unzipFiles(const string zipFile, const string dest)
{
	const int argc = 5;
	char **argv = new char*[argc];
	for (int i = 0; i < argc; i++) {		
		argv[i] = new char[STR_LENGTH];
	}
	
	strcpy(argv[0],"-v");	
	strcpy(argv[1],"-o");
	strcpy(argv[2],"-d");
	strcpy(argv[3],dest.c_str());
	strcpy(argv[4],zipFile.c_str());
	
	
	/* call the actual UnZip routine (string-arguments version) */
	int result = UzpMain(argc, argv);
	
	for (int i = 0; i < argc; i++) {		
		delete argv[i];
		argv[i]=NULL;		
	}
	
	delete [] argv;
	argv = NULL;
	
	return result;	
}

void Utils::writeInt(int socketHandle, const int value)
{
	const int SZ = 4;
	unsigned char byteBuffer[SZ];
	byteBuffer[0] = value >> 24;
	byteBuffer[1] = (value >> 16) & 0xff;
	byteBuffer[2] = (value >> 8) & 0xff;
	byteBuffer[3] = value & 0xff;
	
	int bytesWritten = send(socketHandle, byteBuffer, SZ, SEND_FLAG);
	if (bytesWritten != SZ) {
		printf("Error writing int\n");
	}
};

int Utils::readInt(int socketHandle) 
{
	const int SZ = 4;
	
	unsigned char byteBuffer[SZ];
	memset(byteBuffer,0,SZ);
	
	int bytesReceived = recv(socketHandle, byteBuffer, SZ, MSG_WAITALL );
	if (bytesReceived != SZ) {
		printf("Error reading int\n");
	}
	
	bytesReceived = (int)(byteBuffer[0] << 24 |
						  byteBuffer[1] << 16 |
						  byteBuffer[2] << 8 |
						  byteBuffer[3]);
	return bytesReceived;
	
};

long Utils::readLong(int socketHandle) 
{
	const int SZ = 8;
	
	unsigned char byteBuffer[SZ];
	memset(byteBuffer,0,SZ);
	
	long bytesReceived = recv(socketHandle, byteBuffer, SZ, MSG_WAITALL );
	if (bytesReceived != SZ) {
		printf("Error reading long\n");
	}
	/* discard 0..3 cos long is only 4 bytes */
	bytesReceived = (long)(byteBuffer[4] << 24 |
						   byteBuffer[5] << 16 |
						   byteBuffer[6] << 8 |
						   byteBuffer[7]);
	return bytesReceived;
};

void Utils::writeLong(int socketHandle, const long v)
{
	const int SZ = 8;
	
	unsigned char byteBuffer[SZ];
	memset(byteBuffer,0,SZ);

	/* first four are dummies cos a long, unlike java, is 4 bytes */
	byteBuffer[0] = (unsigned char)(0x00);
	byteBuffer[1] = (unsigned char)(0x00);
	byteBuffer[2] = (unsigned char)(0x00);
	byteBuffer[3] = (unsigned char)(0x00);  
	byteBuffer[4] = (unsigned char)(0xff & (v >> 24));	
	byteBuffer[5] = (unsigned char)(0xff & (v >> 16));
	byteBuffer[6] = (unsigned char)(0xff & (v >>  8));
	byteBuffer[7] = (unsigned char)(0xff &  v);
	
	int bytesWritten = send(socketHandle, byteBuffer, SZ, SEND_FLAG);
	if (bytesWritten != SZ) {
		printf("Error writing long\n");
	}
};

short Utils::readShort(int socketHandle) 
{
	const int SZ = 2;
	
	unsigned char byteBuffer[SZ];
	memset(byteBuffer,0,SZ);
	
	int bytesReceived = recv(socketHandle, byteBuffer, SZ, MSG_WAITALL );
	if (bytesReceived != SZ) {
		printf("Error reading short\n");
	}
	
	short result = (short)(byteBuffer[0] << 8 |
						   byteBuffer[1]);
	return result;
	
};

void Utils::writeShort(int socketHandle, const short value)
{	
	const int SZ = 2;
	unsigned char byteBuffer[SZ];
	byteBuffer[0] = value >> 8;
	byteBuffer[1] = value & 0xff;
	
	int bytesWritten = send(socketHandle, byteBuffer, SZ, SEND_FLAG);
	if (bytesWritten != SZ) {		
		printf("Error writing short\n");
	}
	
};

unsigned char Utils::readByte(int socketHandle) 
{
	const int SZ = 1;
	
	unsigned char byteBuffer[SZ];
	
	int bytesReceived = recv(socketHandle, byteBuffer, SZ, MSG_WAITALL );
	if (bytesReceived != SZ) {
		printf("Error reading byte\n");
	}
	
	return byteBuffer[0];	
};

int Utils::readBytes(int socketHandle, unsigned char *buf, int count) 
{
	
	int bytesReceived = recv(socketHandle, buf, count, MSG_WAITALL );
	return bytesReceived;	
};

void Utils::writeByte(int socketHandle, const unsigned char value)
{
	const int SZ = 1;
	unsigned char byteBuffer[SZ];
	byteBuffer[0] = value;
	
	int bytesWritten = send(socketHandle, byteBuffer, SZ, SEND_FLAG);
	if (bytesWritten != SZ) {
		printf("Error writing byte\n");
	}
	
};

void Utils::writeBytes(int socketHandle, const unsigned char *buf, int count)
{	
	int bytesWritten = send(socketHandle, buf, count, SEND_FLAG);
	if (bytesWritten != count) {
		printf("Error writing bytes\n");
	}
	
};

string Utils::readString(int socketHandle)
{
	string result = string(); 
	int length = (int)readShort(socketHandle);
	
	if (length<0) {		
		printf("Error reading string length:%d\n",length);
		length=0;
		return result;		
	}
	char strBytes[STR_LENGTH];	
	
	int bytesReceived = recv(socketHandle, strBytes, length, MSG_WAITALL );
	if (bytesReceived != length) {
		printf("Error reading string\n");
		return result;
	}	
	strBytes[length]='\0';
	result.assign(strBytes);	
	
	return result;
}

void Utils::writeString(int socketHandle, const string str)
{
	writeShort(socketHandle,str.length());
	int bytesWritten = send(socketHandle, str.c_str(), str.length(), SEND_FLAG);
	if (bytesWritten !=  (int)str.length()) {
		printf("Error writing string\n");
	}
};


void Utils::sendSignal(int socketHandle, const int value)
{
	writeInt(socketHandle,value);
};

char* Utils::allocateChars(const int length) 
{
	char *result = new char[length+1];
  
	memset(result,0,length * sizeof(char));
  
	return result;  
}


#ifdef MULTI_THREADED
void* Utils::start_point(void * at) 
{
  Runnable *runnable_thread = (Runnable *)at;
  
	runnable_thread->run();
	return runnable_thread ;	
}

void Utils::start_thread(void * at) 
{
	/* now start new thread */
	pthread_attr_t tattr;
	pthread_t tid;
	int ret;
	
	/* initialize attr with default attributes */
	pthread_attr_init(&tattr);
		
	/* default behavior*/
	ret = pthread_create(&tid, &tattr,&start_point, at);			
}
#endif

string Utils::firstWord(string s) 
{
  string strCopy = string(s.c_str());
  int pos = strCopy.find(' ',0);
  if (pos > 0) {	
	strCopy.erase(pos,s.length()-pos);
  }
  
  strCopy.assign(Utils::trim(strCopy));
  
  return strCopy;  
}

/**
 * This methods counts the number of characters contained in delim
 * which occurs in the string s. This value is normally
 * the (number_of_spaces + 1).
 */
int Utils::countWords(string original) 
{
	string s = string(original.c_str());
	s.assign(Utils::trim(s));	
  
	int count = 1;  
	for (int i = 0; i < ((int)s.length()-1); i++) {
		if (s.at(i)==' ') {		
			count++;		
			// now skip to beginning of next word in
			// case there are multiple space
			while((s.at(i)==' ')&&(i < ((int)s.length()-1)))
				i++;		
		}	
	}  	
	
	return count;
}

string Utils::trim(string str) 
{  
  // remove leading spaces 
  while(str.at(0)==' ')
	str.erase(0,1);
  // remove trailing spaces 
  while((str.length()>0)&&
		(str.at(str.length()-1)==' '))
	str.erase(str.length()-1,1);  
  return str;  
}

void Utils::sleep(int milliseconds) 
{
  printf("Sleeping :%d\n",milliseconds);
  
  int seconds;
  struct timeval timeVal;

  timeVal.tv_sec = 0;


  if (milliseconds < 1000) {
    timeVal.tv_usec = milliseconds*1000;
    select(0,NULL,NULL,NULL, &timeVal);
  }  
  else {
	seconds = (milliseconds/1000);
	for (int i = 0; i < seconds; i++) {
	  timeVal.tv_usec = 99999;
	  select(0,NULL,NULL,NULL, &timeVal);	  
	}	
	/* now sleep for the rounded off seconds */
	timeVal.tv_usec = (milliseconds-(seconds*1000))*1000;
	select(0,NULL,NULL,NULL, &timeVal);	
  }
  
}

