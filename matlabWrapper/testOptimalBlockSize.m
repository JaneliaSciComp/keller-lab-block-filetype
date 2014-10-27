%test speed vs compression for different block sizes
function [results] = testOptimalBlockSize(im)

%main options (you can use default if you want to simplify)
numThreads = -1;
pixelSize = [];
compressionType = [];


if( isempty(im) )%generate random image
    imSize = [950 784 400];
    im = uint32( reshape( [1:prod(imSize)], imSize) );
else
    imSize = size(im);
end

%temp filename
basename = [tempname];
filenameB = [basename  '.klb'];
%number of bytes per pixel
qq = whos('im');
bytesPerPixel = qq.bytes / prod(imSize);

%results = nan(8*8*16,6);
results = nan(4*16*1,6);
count = 0;
for bx = linspace(8,128,16)
    %for by = linspace(8,128,16)
    by = bx;
        for bz = linspace(8,32,4)
            
            
            blockSize = [bx by bz]
            
            
            
            tic;            
            writeKLBstack(im, filenameB ,numThreads,pixelSize,blockSize,compressionType);
            tt = toc;
            s = dir(filenameB);
            s = s.bytes / 2^10;
            tic;
            aux = readKLBstack(filenameB, numThreads);
            ttR = toc;
            
            
            
            count = count + 1;
            results(count,:) = [bx by bz tt s ttR];%time(secs) , size(in KB)                                           
           
        end        
        save('blockSizeResults\temp.mat','results');
    end
%end
save('blockSizeResults\temp.mat','results');