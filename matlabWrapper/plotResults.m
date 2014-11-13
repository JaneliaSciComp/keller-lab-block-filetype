function plotResults(results, bz,dim)


pos = find( results(:,3) == bz );



XI = results(pos,1);
YI = results(pos,2);

nx = length(unique(XI));
ny = length(unique(YI));

XI = reshape(XI,[nx ny]);
YI = reshape(YI,[nx ny]);

Z = reshape(results(pos,dim),[nx ny]);

if( dim == 5 )
    Z = Z / 2^10;
end

figure;
imagesc(Z,'XData',XI(1,:),'YData', YI(:,1)');
xlabel('Block size along x (pixels)');
ylabel('Block size along y (pixels)');
colormap(jet(256));
caxis([min(Z(:)) prctile(Z(:),50)]);
colorbar;

switch(dim)
    case 4 
        title(['Writing speed for bz = ' num2str(bz)]);
    case 5
        title(['File size (MB) for bz = ' num2str(bz)]);
    case 6
        title(['Reading speed for bz = ' num2str(bz)]);
end