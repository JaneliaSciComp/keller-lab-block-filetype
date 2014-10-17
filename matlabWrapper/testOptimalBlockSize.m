%test speed vs compression for different block sizes
function results = testOptimalBlockSize(im)

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
filename = [tempname '.klb'];

%number of bytes per pixel
qq = whos('im');
bytesPerPixel = qq.bytes / prod(imSize);

results = nan(8^3,5);
count = 0;
for bx = linspace(32,256,8)
    for by = linspace(32,256,8)
        for bz = linspace(32,256,8)
            
            blockSize = [bx by bz];
            
            
            if( prod(blockSize) * bytesPerPixel / 2^10 >= 100 ) 
                tic;
                writeKLBstack(im, filename ,numThreads,pixelSize,blockSize,compressionType);
                tt = toc;
                s = dir(filename);
                s = s.bytes;
            else
                tt = nan;
                s = nan;
            end
            
            
            
            count = count + 1;
            results(count,:) = [bx by bz tt s];%time(secs) , size(in KB)
        end
    end
end


