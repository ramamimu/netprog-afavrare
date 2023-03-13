#!/bin/env python

import socket
import sys
from bs4 import BeautifulSoup

class HTTPResource:

    http_header_delimiter = b'\r\n\r\n'
    content_length_field = b'Content-Length:'

    @classmethod
    def get(cls, host, resource):
        '''
        Creates a new HTTPResource with the given host and request, then tries
        to resolve the host, send the request and receive the response. The
        downloaded HTTPResource is then returned.
        '''
        http = cls(host, resource)
        port = 80
        try:
            ip = socket.gethostbyname(host)
            with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as mysock:
                mysock.connect((ip, port))
                http.send(mysock)
                
                print("\n[Loading Web]") 
                print("Website : {} ({})\n".format(host, ip))
                http.recv(mysock)
        except Exception as e:
            raise e
        return http

    @classmethod
    def read_until(cls, sock, condition, length_start=0, chunk_size=4096):
        '''
        Reads from the given socket until the condition returns True. Returns
        an array of bytes read from the socket.
        The condition should be a function that takes two parameters,
        condition(length, data), where length is the total number of bytes
        read and data is the most recent chunk of data read. Based on those two
        values, the condition must return True in order to stop reading from
        the socket and return the data read so far.
        '''
        data = bytes()
        chunk = bytes()
        length = length_start
        try:
            while not condition(length, chunk):
                chunk = sock.recv(chunk_size)
                if not chunk:
                    break
                else:
                    data += chunk
                    length += len(chunk)
        except socket.timeout:
            pass
        return data

    @classmethod
    def formatted_http_request(cls, host, resource, method='GET'):
        '''
        Returns a sequence of bytes representing an HTTP request of the given
        method. Uses self.resource and self.host to build the HTTP headers.
        '''
        request ='{} / HTTP/1.1\r\nHost:{}\r\n\r\n'.format(method,resource,host)                                                     
        # print(request)                                                    
        return request.encode()


    @classmethod
    def separate_header_and_body(cls, data):
        '''
        Returns a the tuple (header, body) from the given array of bytes. If
        the given array doesn't contain the end of header signal then it is
        assumed to be all header.
        '''
        try:
            index = data.index(cls.http_header_delimiter)
        except:
            return (data, bytes())
        else:
            index += len(cls.http_header_delimiter)
            return (data[:index], data[index:])

    @classmethod
    def get_content_length(cls, header):
        '''
        Returns the integer value given by the Content-Length HTTP field if it
        is found in the given sequence of bytes. Otherwise returns 0.
        '''
        for line in header.split(b'\r\n'):
            if cls.content_length_field in line:
                return int(line[len(cls.content_length_field):])
        return 0

    def __init__(self, host, resource):
        self.host = host
        self.resource = resource
        self.header = bytes()
        self.content_length = 0
        self.body = bytes()
 
    def end_of_header(self, length, data):
        '''
        Returns true if data contains the end-of-header marker.
        '''
        return b'\r\n\r\n' in data

    def end_of_content(self, length, data):
        '''
        Returns true if length does not fullfil the content_length.
        '''
        return self.content_length <= length

    def send(self, sock, method='GET'):
        '''
        Write an HTTP request, with the given method, to the given socket. Uses
        self.http_request to build the HTTP headers.
        '''
        sock.sendall(self.formatted_http_request(self.host,self.resource,method))

    def recv(self, sock):
        '''
        Reads an HTTP Response from the given socket. Returns that response as a
        tuple (header, body) as two sequences of bytes.
        '''
        # read until at end of header
        self.data = self.read_until(sock, self.end_of_header)

        # separate our body and header
        self.header, self.body = self.separate_header_and_body(self.data)

        # get the Content Length from the header
        self.content_length = self.get_content_length(self.header)

        # read until end of Content Length
        self.body += self.read_until(sock, self.end_of_content, len(self.body))

        return (self.header, self.body)

if __name__ == '__main__':
    print("[Afavrare Browser]")
    host = input("Enter host name: ")
    
    # send the request and get the response
    response = HTTPResource.get(host, "/")

    header_output = response.header
    body_output = response.body

    print("[Header]")
    sys.stdout.buffer.write(header_output)
    
    print("[Body]")
    sys.stdout.buffer.write(body_output)

    soup = BeautifulSoup(response.body, "html5lib")
    
    links = []
    for link in soup.findAll('a'):
        links.append(link.get('href'))

    print("\n[Clickable Links]") 
    print(links)