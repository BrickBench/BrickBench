package com.opengg.loader.editor.components;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;


public class JSplitPaneNoDivider
        extends JSplitPane
{
    private int dividerDragSize = 9;

    private int dividerDragOffset = 4;

    public JSplitPaneNoDivider() {
        this( HORIZONTAL_SPLIT );
    }

    public JSplitPaneNoDivider(int orientation ) {
        super( orientation );
        setContinuousLayout( true );
        setDividerSize( 1 );
    }

    public int getDividerDragSize() {
        return dividerDragSize;
    }

    public void setDividerDragSize( int dividerDragSize ) {
        this.dividerDragSize = dividerDragSize;
        revalidate();
    }

    public int getDividerDragOffset() {
        return dividerDragOffset;
    }

    public void setDividerDragOffset( int dividerDragOffset ) {
        this.dividerDragOffset = dividerDragOffset;
        revalidate();
    }
    public void doLayout(){
        super.doLayout();
        // increase divider width or height
        BasicSplitPaneDivider divider = ((BasicSplitPaneUI)getUI()).getDivider();
        Rectangle bounds = divider.getBounds();
        if( orientation == HORIZONTAL_SPLIT ) {
            bounds.x -= dividerDragOffset;
            bounds.width = dividerDragSize;
        } else {
            bounds.y -= dividerDragOffset;
            bounds.height = dividerDragSize;
        }
        divider.setBounds( bounds );
    }

    @Override
    public void updateUI() {
        setUI( new SplitPaneWithZeroSizeDividerUI() );
        revalidate();
    }

    private class SplitPaneWithZeroSizeDividerUI
            extends BasicSplitPaneUI
    {
        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            return new ZeroSizeDivider( this );
        }
    }

    private class ZeroSizeDivider
            extends BasicSplitPaneDivider
    {
        public ZeroSizeDivider( BasicSplitPaneUI ui ) {
            super( ui );
            super.setBorder( null );
            setBackground( UIManager.getColor( "controlShadow" ) );
        }

        @Override
        public void setBorder( Border border ) {
            // ignore
        }

        @Override
        public void paint( Graphics g ) {
            g.setColor( getBackground() );
            if( orientation == HORIZONTAL_SPLIT )
                g.drawLine( dividerDragOffset, 0, dividerDragOffset, getHeight() - 1 );
            else
                g.drawLine( 0, dividerDragOffset, getWidth() - 1, dividerDragOffset );
        }

        @Override
        protected void dragDividerTo( int location ) {
            super.dragDividerTo( location + dividerDragOffset );
        }

        @Override
        protected void finishDraggingTo( int location ) {
            super.finishDraggingTo( location + dividerDragOffset );
        }
    }
}

