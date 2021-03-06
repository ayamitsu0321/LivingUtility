package Schr0.LivingUtility.mods.entity;

import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import Schr0.LivingUtility.mods.LivingUtility;
import Schr0.LivingUtility.mods.entity.ai.EntityLivingUtilityAICollectItem;
import Schr0.LivingUtility.mods.entity.ai.EntityLivingUtilityAIFindChest;
import Schr0.LivingUtility.mods.entity.ai.EntityLivingUtilityAIFollowOwner;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class EntityLivingChest extends EntityLivingUtility
{
	//蓋の開閉の変数(独自)
	private float							prev;
	private float							lid;
	
	//蓋の開閉角度の変数(独自)
	private float							prevLidAngle;
	private float							lidAngle;
	
	//AIの宣言
	//追従				(3)
	//自由行動			(1)
	//アイテム回収		(2)
	//チェストの走査	(2)
	public EntityLivingUtilityAIFollowOwner	AIFollowOwner	= new EntityLivingUtilityAIFollowOwner(this, 1.25F, 2.0F, 2.0F);
	public EntityAIWander					AIWander		= new EntityAIWander(this, 1.25F);
	public EntityLivingUtilityAICollectItem	AICollectItem	= new EntityLivingUtilityAICollectItem(this, 1.25F);
	public EntityLivingUtilityAIFindChest	AIFindChest		= new EntityLivingUtilityAIFindChest(this, 1.25F);
	
	public EntityLivingChest(World par1World)
	{
		super(par1World);
		this.setSize(0.9F, 1.35F);
		this.getNavigator().setAvoidsWater(true);
		
		//AIの切り替えの処理(独自)
		if (par1World != null && !par1World.isRemote)
		{
			this.setAITask();
		}
	}
	
	//内部インベントリの大きさ（abstract独自）
	@Override
	public int getLivingInventrySize()
	{
		return 27;
	}
	
	//AIの切り替えの処理(abstract独自)
	@Override
	public void setAITask()
	{
		super.setAITask();
		
		//AIの除去 AIMoveTowardsRestriction
		this.tasks.removeTask(AIFollowOwner);
		this.tasks.removeTask(AIWander);
		this.tasks.removeTask(AICollectItem);
		this.tasks.removeTask(AIFindChest);
		
		//飼いならし状態の場合
		if (this.isTamed())
		{
			if (this.getMode() == 0)
			{
				//メッセージの出力（独自）
				this.Information(this.getInvName() + " : Follow");
				
				// 5 追従
				this.tasks.addTask(5, AIFollowOwner);
			}
			else if (this.getMode() == 1)
			{
				//メッセージの出力（独自）
				this.Information(this.getInvName() + " : Freedom");
				
				// 5 アイテム回収
				// 6 チェストの走査
				// 7 自由行動
				this.tasks.addTask(5, AICollectItem);
				this.tasks.addTask(6, AIFindChest);
				this.tasks.addTask(7, AIWander);
			}
		}
		//野生状態の場合
		else
		{
			// 5 アイテム回収
			// 6 自由行動
			this.tasks.addTask(5, AICollectItem);
			this.tasks.addTask(6, AIWander);
		}
	}
	
	//蓋の角度（独自）
	@SideOnly(Side.CLIENT)
	public float getCoverAngle(float par1)
	{
		return (this.prevLidAngle + (this.lidAngle - this.prevLidAngle) * par1) * 0.5F * (float) Math.PI;
	}
	
	//属性の付与
	@Override
	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.maxHealth).setAttribute(20.0D);
		this.getEntityAttribute(SharedMonsterAttributes.movementSpeed).setAttribute(0.25D);
	}
	
	//足音
	@Override
	protected void playStepSound(int par1, int par2, int par3, int par4)
	{
		this.playSound("step.wood", 0.25F, 1.0F);
	}
	
	//被ダメージ時の音声
	@Override
	protected String getHurtSound()
	{
		return "dig.wood";
	}
	
	//死亡時の音声
	@Override
	protected String getDeathSound()
	{
		return "random.break";
	}
	
	//インタラクトした際の処理
	@Override
	public boolean interact(EntityPlayer par1EntityPlayer)
	{
		super.interact(par1EntityPlayer);
		
		//手に持っているアイテム
		ItemStack CurrentItem = par1EntityPlayer.inventory.getCurrentItem();
		
		//飼い慣らし状態である場合
		if (this.isTamed())
		{
			//飼い主である場合
			if (par1EntityPlayer.username.equalsIgnoreCase(this.getOwnerName()))
			{
				//手に何も持っていない場合
				if (CurrentItem == null)
				{
					//スニーキング状態である場合
					if (par1EntityPlayer.isSneaking())
					{
						//騎乗の処理（独自）
						this.setMount(par1EntityPlayer);
					}
					//非スニーキング状態の場合
					else
					{
						//クライアントだけの処理
						if (!this.worldObj.isRemote)
						{
							//チェストのGUIを表示
							par1EntityPlayer.displayGUIChest(this);
						}
					}
					
					//Itemを振る動作
					par1EntityPlayer.swingItem();
					return true;
				}
				//手にワンドを持っている場合
				else if (CurrentItem.itemID == LivingUtility.Item_LUWand.itemID)
				{
					int mode = CurrentItem.getItemDamage() - 1;
					
					if (mode == -1)
					{
						//お座りの処理（独自）
						this.setSafeSit();
						
						//Itemを振る動作
						par1EntityPlayer.swingItem();
					}
					else
					{
						//お座り解除
						this.aiSit.setSitting(false);
						
						//ModeをSet
						this.setMode(mode);
						
						//AIの切り替えの処理(独自)
						this.setAITask();
						
						//Itemを振る動作
						par1EntityPlayer.swingItem();
					}
					
					return true;
				}
				//手にマテリアル・キー以外のアイテムを持っている場合
				else if (CurrentItem.itemID != LivingUtility.Item_LUMaterial.itemID && CurrentItem.itemID != LivingUtility.Item_LUKey.itemID)
				{
					//プレーヤーと同じアイテムを所持している場合
					if (this.getHeldItem() != null && this.getHeldItem().isItemEqual(CurrentItem))
					{
						//アイテム所持の解除
						this.setCurrentItemOrArmor(0, null);
						
						//音を出す
						this.playSE("random.orb", 1.0F, 1.0F);
					}
					else
					{
						//手に持たせる
						this.setCurrentItemOrArmor(0, CurrentItem);
						
						//音を出す
						this.playSE("random.pop", 1.0F, 1.0F);
					}
					
					//Itemを振る動作
					par1EntityPlayer.swingItem();
				}
				
				return super.interact(par1EntityPlayer);
			}
			//飼い主でない場合
			else
			{
				//手にキーを持っている場合
				if (CurrentItem != null && CurrentItem.itemID == LivingUtility.Item_LUKey.itemID && CurrentItem.getItemDamage() == 0)
				{
					//NBTタグを取得
					NBTTagCompound nbt = CurrentItem.getTagCompound();
					if (nbt == null)
					{
						nbt = new NBTTagCompound();
						CurrentItem.setTagCompound(nbt);
					}
					
					String OwnerName = nbt.getString("OwnerName");
					
					//オーナーの名前が登録されている場合
					if (OwnerName.length() > 0 && OwnerName.equalsIgnoreCase(this.getOwnerName()))
					{
						//クライアントだけの処理
						if (!this.worldObj.isRemote)
						{
							//チェストのGUIを表示
							par1EntityPlayer.displayGUIChest(this);
						}
						
						return true;
					}
					//オーナーでない場合
					else
					{
						//メッセージの出力（独自）
						this.Information("You are not the master of " + this.getInvName());
						
						//音を出す
						this.playSE("note.bass", 1.0F, 1.0F);
						
						return false;
					}
				}
				
				return super.interact(par1EntityPlayer);
			}
		}
		//飼い慣らし状態でない場合
		else
		{
			//飼い慣らし
			this.setTamed(true);
			this.setOwner(par1EntityPlayer.username);
			
			//元のBlockのItemStackのset（独自）
			ItemStack block = new ItemStack(Block.chest.blockID, 1, 0);
			this.setBlockStack(block);
			
			//メッセージの出力（独自）
			this.Information(this.getInvName() + " : Set Owner : " + par1EntityPlayer.username);
			
			//音を出す
			this.playSE("random.pop", 1.0F, 1.0F);
			
			return super.interact(par1EntityPlayer);
		}
	}
	
	//Entityのアップデート
	@Override
	public void onUpdate()
	{
		super.onUpdate();
		
		//何かに乗っている場合
		if (this.isRiding())
		{
			EntityLivingBase Owner = (EntityLivingBase) this.ridingEntity;
			
			//Ownerと同様の正面を向く
			this.prevRotationYaw = this.rotationYaw = Owner.rotationYaw;
		}
		
		//蓋の角度・音声の設定//
		this.prevLidAngle = this.lidAngle;
		float f = 0.2F;//開閉速度 (0.1F)
		
		if (this.isOpen() && this.lidAngle == 0.0F)
		{
			//音を出す
			this.playSE("random.chestopen", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
			//this.playSE("random.eat", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
		}
		
		if (!this.isOpen() && this.lidAngle > 0.0F || this.isOpen() && this.lidAngle < 1.0F)
		{
			float f1 = this.lidAngle;
			
			if (this.isOpen())
			{
				this.lidAngle += f;
			}
			else
			{
				this.lidAngle -= f;
			}
			
			if (this.lidAngle > 1.0F)
			{
				this.lidAngle = 1.0F;
			}
			
			float f2 = 0.5F;
			
			if (this.lidAngle < f2 && f1 >= f2)
			{
				//音を出す
				this.playSE("random.chestclosed", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
				//this.playSE("random.burp", 0.5F, this.worldObj.rand.nextFloat() * 0.1F + 0.9F);
			}
			
			if (this.lidAngle < 0.0F)
			{
				this.lidAngle = 0.0F;
			}
		}
	}
	
	//生物のアップデート
	@Override
	public void onLivingUpdate()
	{
		super.onLivingUpdate();
		
		//アイテムを拾う判定
		boolean isCollectItem = false;
		
		//アイテムの回収//
		if (!this.worldObj.isRemote && !this.dead)
		{
			List list = this.worldObj.getEntitiesWithinAABB(EntityItem.class, this.boundingBox.expand(0.5D, 0.0D, 0.5D));
			Iterator iterator = list.iterator();
			
			while (iterator.hasNext())
			{
				EntityItem entityitem = (EntityItem) iterator.next();
				
				if (!entityitem.isDead && entityitem.getEntityItem() != null)
				{
					ItemStack itemstack = entityitem.getEntityItem();
					
					//何か持っている場合
					if (this.getHeldItem() != null)
					{
						ItemStack HeldStack = this.getHeldItem().copy();
						
						//持っているアイテムのみ
						if (HeldStack.isItemEqual(itemstack))
						{
							//NBTTagが存在している場合
							if (itemstack.hasTagCompound() && HeldStack.hasTagCompound())
							{
								if (itemstack.stackTagCompound.equals(HeldStack.stackTagCompound))
								{
									//インベントリにアイテムを追加（プレイヤー改変）
									if (this.addItemStackToInventory(itemstack))
									{
										isCollectItem = true;
										
										if (itemstack.stackSize <= 0)
										{
											entityitem.setDead();
										}
									}
								}
							}
							else
							{
								//インベントリにアイテムを追加（プレイヤー改変）
								if (this.addItemStackToInventory(itemstack))
								{
									isCollectItem = true;
									
									if (itemstack.stackSize <= 0)
									{
										entityitem.setDead();
									}
								}
							}
						}
					}
					//何も持っていない場合
					else
					{
						if (this.addItemStackToInventory(itemstack))
						{
							isCollectItem = true;
							
							if (itemstack.stackSize <= 0)
							{
								entityitem.setDead();
							}
						}
					}
					
				}
			}
		}
		
		//開閉の設定//
		this.prev = this.lid;
		float f = 0.4F;//開閉速度 (0.1F)
		
		if (isCollectItem && this.lid == 0.0F)
		{
			//開く
			this.setOpen(true);
			this.lid++;
		}
		
		if (!isCollectItem && this.lid > 0.0F || isCollectItem && this.lid < 1.0F)
		{
			float f1 = this.lid;
			
			if (isCollectItem)
			{
				this.lid += f;
			}
			else
			{
				this.lid -= f;
			}
			
			if (this.lid > 1.0F)
			{
				this.lid = 1.0F;
			}
			
			float f2 = 0.5F;
			
			if (this.lid < f2 && f1 >= f2)
			{
				//閉じる
				this.setOpen(false);
			}
			
			if (this.lid < 0.0F)
			{
				this.lid = 0.0F;
			}
		}
	}
	
}
