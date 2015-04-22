%test all the mex functions
function testMexWrappers(im)

%main options (you can use default if you want to simplify)
numThreads = -1;
pixelSize = [];
blockSize = [96 96 8];
compressionType = 2;


if( isempty(im) )%generate random image
    imSize = [950 784 400];
    im = reshape( [1:prod(imSize)], imSize);
else
    imSize = size(im);
end

%temp filename
filename = [tempname '.klb']

%write file
writeKLBstack(im, filename );
writeKLBstack(im, filename ,numThreads);
writeKLBstack(im, filename ,numThreads,pixelSize);
writeKLBstack(im, filename ,numThreads,pixelSize,blockSize);
writeKLBstack(im, filename ,numThreads,pixelSize,blockSize,compressionType);
writeKLBstack(im, filename ,numThreads,pixelSize,blockSize,compressionType, 'Testing metadata');
disp('Write file passed...')

%read header
header = readKLBheader(filename)

qq = header.blockSize(1:3);
if( norm( qq - blockSize ) > 0 )
    error('Block size in header disagrees');
end

disp('Read header file passed...')

%read full stack
imR = readKLBstack(filename);
qq = single(imR) - single(im);

if( max(abs(qq(:))) > 0 )
    error('Full image read disagrees');
end

disp('Read entire file passed...')

%read planes
numPlanes = 4;
for dim=1:3
    
    planes = floor(linspace(1,imSize(dim), numPlanes));
    
    for kk = planes
        imS = readKLBslice(filename,kk,dim);
        
        switch(dim)
            case 1
                qq = single(squeeze(im(kk,:,:))) - single(imS);               
            case 2
                qq = single(squeeze(im(:,kk,:))) - single(imS);               
            case 3
                qq = single(squeeze(im(:,:,kk))) - single(imS);
        end
        if( max(abs(qq(:))) > 0 )
            [dim kk]
            error('Full image read disagrees');
        end
    end
end

disp('Read XY, XZ, and YZ planes passed...')

%read ROI
ROI = [11 12 15;...
        20 31 22]

patch = readKLBroi(filename, ROI);
qq = im(ROI(1,1):ROI(2,1),ROI(1,2):ROI(2,2),ROI(1,3):ROI(2,3));

if( max(abs(single(patch(:)-qq(:)))) == 0 )
    disp('Read KLB ROI passed...')
else
    disp('ERROR in read KLB ROI')
end