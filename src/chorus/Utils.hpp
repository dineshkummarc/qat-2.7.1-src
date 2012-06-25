/*
 * @author webhiker
 * @version 1.0, 26 April 2000
 *
 */

// ANSI C Headers 
#include <stdlib.h>

#ifndef UTILS_H
#define UTILS_H

#include <algorithm>
#include <fstream>
#include <iostream>
#include <string>

using namespace std;

extern "C" {
#include <fcntl.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/socket.h>
#include <sys/time.h>
#include "unzip.h"
#include "version.h"
#ifdef MULTI_THREADED
#include <pthread.h>
#endif
}

#include "defs.h"
#include "Runnable.hpp"
#include "ConsoleServer.hpp"


class Utils 
{
public:
#ifdef MULTI_THREADED
  static void start_thread(void * at);
  static void* start_point(void * at);
#endif
  static void touch(string fileName);
  static int createH(string fileName);
  static FILE* createF(string fileName);
  static int openH(string fileName);
  static FILE* openF(string fileName);
  static string substring(const string src, int start, int end);
  static int lastIndexOf(const string str, char c);	
  static int mk_dir(const string dirName);
  static int mk_dirp(const string dirName);
  static long size(string fileName);  
  static int exists(string fileName);	
  static int delete_file(string fileName);
  static int unzipFiles(const string zipFile, const string dest);
  static int readChar(char *buff, int count, FILE *file);	
  static int readByte(unsigned char *buff, int count, FILE *file);	
  static int cleanUnzippedFiles(string zipFile, string workDir);
  static int getInt(const unsigned char c[]);
  static long getLong(const unsigned char c[]);
  static void writeInt(int socketHandle, const int value);
  static int readInt(int socketHandle) ;
  static long readLong(int socketHandle) ;
  static void writeLong(int socketHandle, const long v);
  static short readShort(int socketHandle) ;
  static void writeShort(int socketHandle, const short value);
  static unsigned char readByte(int socketHandle) ;
  static int readBytes(int socketHandle, unsigned char *buf, int count) ;
  static void writeByte(int socketHandle, const unsigned char value);
  static void writeBytes(int socketHandle, const unsigned char *buf, int count);
  static string readString(int socketHandle) ;
  static void writeString(int socketHandle, const string str);
  static void sendSignal(int socketHandle, const int value);
  static char* allocateChars(const int length);  
  static string firstWord(string s);  
  static int countWords(string s);  
  static string trim(string str);  
  static void sleep(int milliseconds);
};

#endif
