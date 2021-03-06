package yuuto.quantumelectronics.transport.tile;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import yuuto.quantumelectronics.tile.base.IMachine;
import yuuto.quantumelectronics.transport.ITransportNode;
import yuuto.quantumelectronics.transport.TileGridNode;
import yuuto.quantumelectronics.transport.filter.ItemFilter;
import yuuto.quantumelectronics.transport.grid.PylonGrid;
import yuuto.quantumelectronics.transport.managers.*;
import yuuto.quantumelectronics.transport.routing.*;

public class TileItemRouter extends TileGridNode implements IMachine, ITransportNode, 
		IInventoryParrent{

	int timer = 20;
	ForgeDirection orientation = ForgeDirection.getOrientation(2);
	InventoryFilter filter = new InventoryFilter(9, this);
	int channel = 0;
	int subType;
	IItemRouter router;
	
	public TileItemRouter(){
		super();
	}
	public TileItemRouter(int type){
		super();
		setType(type);
	}
	protected void setType(int type){
		switch(type){
		case 0:
			router = new ItemExtractor(this);
			break;
		case 1:
			router = new ItemReceiver(this);
			break;
		case 2:
			router = new ItemProvider(this);
			break;
		case 3:
			router = new ItemSupplierActive(this);
			break;
		case 4:
			router = new ItemSupplierPassive(this);
			break;
		default:
			break;
		}
		subType = type;
	}
	
	@Override
	public void initialize(){
		super.initialize();
		this.onNeighborChange(worldObj, xCoord, yCoord, zCoord, xCoord, yCoord, xCoord);
	}
	@Override
	public void uninitialize(){
		super.uninitialize();
		this.router.onPreGridChange();
	}
	
	@Override
	public void doWork() {
		timer--;
		if(timer < 1){
			if(router != null)
				router.doWork();
			timer = 20;
		}
	}
	@Override
	public ForgeDirection getOrientation() {
		return orientation;
	}
	@Override
	public ForgeDirection setOrientation(ForgeDirection dir) {
		if(router != null)
			router.setOrientation(dir);
		orientation = dir.getOpposite();
		this.onNeighborChange(worldObj, xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
		return orientation;
	}
	@Override
	public ForgeDirection rotateAround(ForgeDirection axis) {
		orientation = orientation.getRotation(axis);
		if(router != null)
		router.setOrientation(orientation.getOpposite());
		this.onNeighborChange(worldObj, xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
		return orientation;
	}
	@Override
	public boolean isActive() {
		return false;
	}
	@Override
	public void onNeighborChange(IBlockAccess world, int x, int y, int z,
			int tileX, int tileY, int tileZ) {
		if(router == null)
			return;
		TileEntity tile = worldObj.getTileEntity(
				this.xCoord+orientation.offsetX, 
				this.yCoord+orientation.offsetY, 
				this.zCoord+orientation.offsetZ);
		if(tile instanceof IInventory){
			router.setTarget((IInventory) tile);
		}else{
			router.setTarget(null);
		}
		
	}
	@Override
	public void setGrid(PylonGrid grid) {
		if(router != null)
			router.onPreGridChange();
		super.setGrid(grid);
		if(router != null)
			router.onPostGridChange();
	}
	public void setChannel(int channel){
		this.channel = channel;
		if(router != null)
			router.setChannel(channel);
	}
	@Override
	public void onInventoryUpdate(IInventory inventory) {
		if(inventory != this.filter)
			return;
		if(router == null)
			return;
		if(filter.isEmpty()){
			router.setFilter(null);
			return;
		}
		ItemStack[] filt = filter.getItemsFiltered();
		if(filt == null || filt.length < 1){
			router.setFilter(null);
			return;
		}
		System.out.println("reset filter");
		router.setFilter(new ItemFilter(filt, true, false, true, true));
	}
	
	@Override
    public Packet getDescriptionPacket(){
    	NBTTagCompound nbt = new NBTTagCompound();
    	this.writeToNBT(nbt);
    	this.onNeighborChange(worldObj, xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    	return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 1, nbt);
    }
    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity pkt){
    	NBTTagCompound nbt = pkt.func_148857_g();
    	if(nbt == null || nbt.hasNoTags())
    		return;
    	this.readFromNBT(nbt);
    	this.onNeighborChange(worldObj, xCoord, yCoord, zCoord, xCoord, yCoord, zCoord);
    }
	@Override
	public void readFromNBT(NBTTagCompound nbt){
	    super.readFromNBT(nbt);
	    if(router == null)
	    	this.setType(nbt.getInteger("SubType"));
	    this.setChannel(nbt.getInteger("Channel"));
	    orientation = ForgeDirection.getOrientation(nbt.getInteger("Orientation"));
	    if(router != null)
	    	router.setOrientation(orientation.getOpposite());
	    
	    this.filter.readFromNBT(nbt);
	    this.onInventoryUpdate(filter);
	    this.markDirty();
	    
	}
	@Override
	public void writeToNBT(NBTTagCompound nbt){
	    super.writeToNBT(nbt);
	    nbt.setInteger("Orientation", orientation.ordinal());
	    nbt.setInteger("Channel", channel);
	    nbt.setInteger("SubType", subType);
	    filter.writeToNBT(nbt);
	}
	public IInventory getFilter(){
		return filter;
	}
	public boolean isSupplier(){
		return subType == 3 || subType == 4;
	}

}
