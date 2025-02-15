package com.inscripts.cometchatpulse.Repository

import android.app.Activity
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.support.annotation.WorkerThread
import android.util.Log
import android.view.View
import android.widget.Toast
import com.cometchat.pro.constants.CometChatConstants
import com.cometchat.pro.core.Call
import com.cometchat.pro.core.CometChat
import com.cometchat.pro.core.UsersRequest
import com.cometchat.pro.exceptions.CometChatException
import com.cometchat.pro.models.User
import com.facebook.shimmer.ShimmerFrameLayout
import com.inscripts.cometchatpulse.CometChatPro
import com.inscripts.cometchatpulse.Utils.CommonUtil
import com.cometchat.pro.core.BlockedUsersRequest
import com.cometchat.pro.models.GroupMember
import com.inscripts.cometchatpulse.Activities.MainActivity
import java.lang.Exception
import java.util.ArrayList


class UserRepository {

    var usersList = MutableLiveData<MutableMap<String, User>>()

    var filterUsersList = MutableLiveData<MutableMap<String, User>>()

    var user = MutableLiveData<User>()

    var blockedUser = MutableLiveData<User>()

    var mutableUserList = mutableMapOf<String, User>()

    var mutableFilterUserList= mutableMapOf<String,User>()

    var mutableBlockedUserList = mutableMapOf<String, User>()

    private var blockedUserList:MutableLiveData<MutableMap<String,User>> = MutableLiveData()

    var userRequest: UsersRequest? = null

    var unreadCountmap:MutableMap<String,Int> = mutableMapOf()

    var liveUnreadCountMap:MutableLiveData<MutableMap<String,Int>> = MutableLiveData()

     private val TAG = "UserRepository"

    @WorkerThread
    fun fetchUsers(LIMIT: Int, shimmer: ShimmerFrameLayout?) {

        if (userRequest == null) {

            userRequest = UsersRequest.UsersRequestBuilder().setLimit(LIMIT).build()

            userRequest?.fetchNext(object : CometChat.CallbackListener<List<User>>() {

                override fun onError(p0: CometChatException?) {
                    Toast.makeText(CometChatPro.applicationContext(), p0?.message, Toast.LENGTH_SHORT).show()
                    shimmer?.stopShimmer()
                    shimmer?.visibility = View.GONE
                }

                override fun onSuccess(p0: List<User>?) {
                    if (p0 != null) {
                        Log.d("UsersRequest", " " + p0.size)
                        for (user: User in p0) {
                            mutableUserList.put(user.uid, user)
                        }
                        shimmer?.stopShimmer()
                        shimmer?.visibility = View.GONE
                        usersList.value = mutableUserList
                    }
                }


            })


        } else {

            userRequest?.fetchNext(object : CometChat.CallbackListener<List<User>>() {

                override fun onSuccess(p0: List<User>?) {
                    Log.d("UsersRequest", " " + p0?.size)
                    if (p0 != null) {
                        for (user: User in p0) {
                            mutableUserList.put(user.uid, user)
                        }
                        usersList.value = mutableUserList
                    }

                }

                override fun onError(p0: CometChatException?) {
                    Log.d("fetchNext", "UsersRequest onError: ${p0?.message}")
                }

            })

        }

    }


    @WorkerThread
    fun addPresenceListener(listener: String) {

        CometChat.addUserListener(listener, object : CometChat.UserListener() {
            override fun onUserOnline(p0: com.cometchat.pro.models.User?) {

                if (p0 != null) {
                    mutableUserList.put(p0.uid, p0)
                    usersList.value = mutableUserList
                }
            }

            override fun onUserOffline(p0:User?) {
                if (p0 != null) {
                    mutableUserList.put(p0.uid, p0)
                    usersList.value = mutableUserList
                }
            }

        })

    }

    @WorkerThread
    fun clearjob() {

    }

    fun removeUserListener(listener: String) {
        CometChat.removeUserListener(listener)
    }

    fun initCall(context: Context, call: Call) {
        CometChat.initiateCall(call, object : CometChat.CallbackListener<Call>() {

            override fun onSuccess(p0: Call?) {
                CommonUtil.startCallIntent(CometChatConstants.RECEIVER_TYPE_USER, context, p0?.callReceiver as User, p0.type, true, p0.sessionId)
            }

            override fun onError(p0: CometChatException?) {
                Log.d("initiateCall", "onError: ${p0?.message}")
            }

        })
    }

    fun blockUser(uidList: MutableList<String>) {

        CometChat.blockUsers(uidList,object:CometChat.CallbackListener<HashMap<String,String>>() {

            override fun onSuccess(resultMap: HashMap<String, String>) {
                Toast.makeText(CometChatPro.applicationContext(),"Blocked Successfully",Toast.LENGTH_SHORT).show()
            }

            override fun onError(e: CometChatException) {
               Log.d(TAG,"blockUsers onError: ")
            }
        })
    }

    fun getBlockedUser(limit: Int):MutableLiveData<MutableMap<String,User>> {

        val blockedUsersRequest = BlockedUsersRequest.BlockedUsersRequestBuilder()
                .setLimit(limit)
                .build()

        blockedUsersRequest.fetchNext(object :CometChat.CallbackListener<List<User>>(){
            override fun onSuccess(p0: List<User>?) {

                if (p0!=null) {
                    for (user in p0) {
                       mutableBlockedUserList.put(user.uid,user)
                    }
                    blockedUserList.value= mutableBlockedUserList
                }

            }

            override fun onError(p0: CometChatException?) {
               Log.d(TAG,"onError: ")
            }

        })

        return blockedUserList
    }

    fun unBlockUser(user: User) {

         val uidList:MutableList<String> = ArrayList()
         uidList.add(user.uid)

        CometChat.unblockUsers(uidList,object:CometChat.CallbackListener<HashMap<String,String>>(){

            override fun onSuccess(p0: HashMap<String, String>?) {
                  try {
                      if (p0?.containsKey(user.uid)!!){
                          blockedUser.value=user
                      }
                  }catch (e:Exception){
                      e.printStackTrace()
                  }

            }

            override fun onError(p0: CometChatException?) {
                Log.d(TAG,"onError: "+p0?.message)
            }

        })

    }

    fun searchUser(userName: String) {

            mutableFilterUserList.clear()
               userRequest=null
            val searchUserRequest = UsersRequest.UsersRequestBuilder().setLimit(100).setSearchKeyword(userName).build()
            searchUserRequest?.fetchNext(object : CometChat.CallbackListener<List<User>>() {
                override fun onSuccess(p0: List<User>?) {
                        if (p0 != null) {
                            for (user: User in p0) {
                                mutableFilterUserList[user.uid] = user
                            }
                            filterUsersList.value=mutableFilterUserList
                        }
                }
                override fun onError(p0: CometChatException?) {
                    Log.d(TAG, "search onError: ${p0?.message}")
                }

            })
    }

    fun getUnreadCount():MutableLiveData<MutableMap<String,Int>> {

        CometChat.getUnreadMessageCountForAllUsers(object : CometChat.CallbackListener<HashMap<String, Int>>() {
            override fun onSuccess(stringIntegerHashMap: HashMap<String, Int>) {
                Log.d(TAG,"onSuccess: ${stringIntegerHashMap.size}")
                 unreadCountmap=stringIntegerHashMap
                 liveUnreadCountMap.value=unreadCountmap

            }
            override fun onError(e: CometChatException) {
                Log.d(TAG,"onError: ${e.message}")
            }

        })

        return liveUnreadCountMap
    }

    fun addMember(activity:Activity,guidList: MutableSet<String>, guid: String) {

        val member:MutableList<GroupMember> = arrayListOf()
        guidList.forEach { id->
            member.add(GroupMember(id,CometChatConstants.SCOPE_PARTICIPANT))
        }

        CometChat.addMembersToGroup(guid,member,null,object :CometChat.CallbackListener<HashMap<String,String>>(){

            override fun onSuccess(p0: HashMap<String, String>?) {
               Toast.makeText(CometChatPro.applicationContext(),"Success",Toast.LENGTH_SHORT).show()
                activity?.onBackPressed()
            }

            override fun onError(p0: CometChatException?) {
             Log.d(TAG,"addMembersToGroup onError: $p0")
            }

        })

    }
}