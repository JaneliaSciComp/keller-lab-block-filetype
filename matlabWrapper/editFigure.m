%Opens a figure and adjusts all the font sizes for better display
%It also saves a copy in png mode
function editFigure(h, fSize, fSizeTitle, fSizeLegend)

%fSize=18;
%fSizeTitle=14;
%fSizeLegend=14;

%h=openfig([figBasename '.fig'],'new');
figure(h);

%get(gcf,'CurrentAxes')
%find all the existing axes
ss=findobj(h,'Type','axes','-not','tag','legend');

for kk=1:length(ss)
    axes(ss(kk));
    %change axis size
    set(gca,'FontSize',fSize);
    %change title
    t = get(gca, 'title');
    set(t, 'FontSize', fSizeTitle);
    
    %change axis labels
    t = get(gca, 'xlabel');
    set(t, 'FontSize', fSize);
    t = get(gca, 'ylabel');
    set(t, 'FontSize', fSize);
end


%restore legend in front
ss=findobj(h,'Type','axes','-and','tag','legend');
for kk=1:length(ss)
    axes(ss(kk));
    set(gca,'FontSize',fSizeLegend);
end
